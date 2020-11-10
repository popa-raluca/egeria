/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright Contributors to the ODPi Egeria project. */
package org.odpi.openmetadata.accessservices.subjectarea.fvt;

import org.odpi.openmetadata.accessservices.subjectarea.client.SubjectAreaNodeClient;
import org.odpi.openmetadata.accessservices.subjectarea.client.SubjectAreaRestClient;
import org.odpi.openmetadata.accessservices.subjectarea.client.nodes.terms.SubjectAreaTermClient;
import org.odpi.openmetadata.accessservices.subjectarea.properties.classifications.Confidence;
import org.odpi.openmetadata.accessservices.subjectarea.properties.classifications.Confidentiality;
import org.odpi.openmetadata.accessservices.subjectarea.properties.classifications.Criticality;
import org.odpi.openmetadata.accessservices.subjectarea.properties.classifications.Retention;
import org.odpi.openmetadata.accessservices.subjectarea.properties.enums.ConfidenceLevel;
import org.odpi.openmetadata.accessservices.subjectarea.properties.enums.CriticalityLevel;
import org.odpi.openmetadata.accessservices.subjectarea.properties.enums.RetentionBasis;
import org.odpi.openmetadata.accessservices.subjectarea.properties.objects.category.Category;
import org.odpi.openmetadata.accessservices.subjectarea.properties.objects.common.FindRequest;
import org.odpi.openmetadata.accessservices.subjectarea.properties.objects.common.GovernanceActions;
import org.odpi.openmetadata.accessservices.subjectarea.properties.objects.glossary.Glossary;
import org.odpi.openmetadata.accessservices.subjectarea.properties.objects.graph.Line;
import org.odpi.openmetadata.accessservices.subjectarea.properties.objects.nodesummary.CategorySummary;
import org.odpi.openmetadata.accessservices.subjectarea.properties.objects.nodesummary.GlossarySummary;
import org.odpi.openmetadata.accessservices.subjectarea.properties.objects.term.Term;
import org.odpi.openmetadata.frameworks.connectors.ffdc.InvalidParameterException;
import org.odpi.openmetadata.frameworks.connectors.ffdc.PropertyServerException;
import org.odpi.openmetadata.frameworks.connectors.ffdc.UserNotAuthorizedException;
import org.odpi.openmetadata.repositoryservices.connectors.stores.metadatacollectionstore.properties.SequencingOrder;

import java.io.IOException;
import java.util.*;

/**
 * FVT resource to call subject area term client API
 */
public class TermFVT {
    private static final String DEFAULT_TEST_GLOSSARY_NAME = "Test Glossary for term FVT";
    private static final String DEFAULT_TEST_TERM_NAME = "Test term A";
    private static final String DEFAULT_TEST_TERM_NAME_UPDATED = "Test term A updated";
    private SubjectAreaNodeClient<Term> subjectAreaTerm = null;
    private SubjectAreaTermClient subjectAreaTermClient = null;
    private GlossaryFVT glossaryFVT =null;
    private CategoryFVT categoryFVT =null;
    private SubjectAreaDefinitionCategoryFVT subjectAreaFVT =null;
    private String userId =null;
    private int existingTermCount = 0;
    /*
     * Keep track of all the created guids in this set, by adding create and restore guids and removing when deleting.
     * At the end of the test it will delete any remaining guids.
     *
     * Note this FVT is called by other FVTs. Who ever constructs the FVT should run deleteRemainingTerms.
     */
    private Set<String> createdTermsSet = new HashSet<>();

    public static void main(String args[])
    {
        try
        {
            String url = RunAllFVTOn2Servers.getUrl(args);
            runWith2Servers(url);
        } catch (IOException e1)
        {
            System.out.println("Error getting user input");
        } catch (SubjectAreaFVTCheckedException e) {
            System.out.println("ERROR: " + e.getMessage() );
        } catch (UserNotAuthorizedException | InvalidParameterException | PropertyServerException e) {
            System.out.println("ERROR: " + e.getReportedErrorMessage() + " Suggested action: " + e.getReportedUserAction());
        }

    }
    public TermFVT(String url,String serverName,String userId) throws InvalidParameterException, PropertyServerException, UserNotAuthorizedException {
        SubjectAreaRestClient client = new SubjectAreaRestClient(serverName, url);
        subjectAreaTerm = new SubjectAreaTermClient<>(client);
        subjectAreaTermClient = (SubjectAreaTermClient)subjectAreaTerm;

        System.out.println("Create a glossary");
        glossaryFVT = new GlossaryFVT(url,serverName,userId);
        categoryFVT = new CategoryFVT(url, serverName,userId);
        subjectAreaFVT = new SubjectAreaDefinitionCategoryFVT(url, serverName,userId);

        this.userId=userId;
        existingTermCount = findTerms(".*").size();
        System.out.println("existingTermCount " + existingTermCount);
    }
    public static void runWith2Servers(String url) throws SubjectAreaFVTCheckedException, InvalidParameterException, PropertyServerException, UserNotAuthorizedException {
        runIt(url, FVTConstants.SERVER_NAME1, FVTConstants.USERID);
        runIt(url, FVTConstants.SERVER_NAME2, FVTConstants.USERID);
    }

    public static void runIt(String url, String serverName, String userId) throws  SubjectAreaFVTCheckedException, InvalidParameterException, PropertyServerException, UserNotAuthorizedException {
        System.out.println("TermFVT runIt started");
        TermFVT fvt =new TermFVT(url,serverName,userId);
        fvt.run();
        fvt.deleteRemaining();
        System.out.println("TermFVT runIt stopped");
    }
    public static int getTermCount(String url, String serverName, String userId) throws InvalidParameterException, UserNotAuthorizedException, PropertyServerException, SubjectAreaFVTCheckedException  {
        TermFVT fvt = new TermFVT(url, serverName, userId);
        return fvt.findTerms(".*").size();
    }

    public void run() throws SubjectAreaFVTCheckedException, InvalidParameterException, PropertyServerException, UserNotAuthorizedException {
        Glossary glossary= glossaryFVT.createGlossary(DEFAULT_TEST_GLOSSARY_NAME);
        System.out.println("Create a term1");
        String glossaryGuid = glossary.getSystemAttributes().getGUID();
        Term term1 = createTerm(DEFAULT_TEST_TERM_NAME, glossaryGuid);
        FVTUtils.validateNode(term1);
        System.out.println("Create a term2 using glossary userId");
        Term term2 = createTerm(DEFAULT_TEST_TERM_NAME, glossaryGuid);
        FVTUtils.validateNode(term2);
        System.out.println("Create a term2 using glossary userId");

        FindRequest findRequest = new FindRequest();
        List<Term> results = glossaryFVT.getGlossaryTerms(glossaryGuid, findRequest);
        if (results.size() != 2) {
            throw new SubjectAreaFVTCheckedException("ERROR: Expected 2 back on getGlossaryTerms " + results.size());
        }
        findRequest.setPageSize(1);
        results = glossaryFVT.getGlossaryTerms(glossaryGuid, findRequest);
        if (results.size() != 1) {
            throw new SubjectAreaFVTCheckedException("ERROR: Expected 1 back on getGlossaryTerms with page size 1" + results.size());
        }

        Term termForUpdate = new Term();
        termForUpdate.setName(DEFAULT_TEST_TERM_NAME_UPDATED);
        System.out.println("Get term1");
        String guid = term1.getSystemAttributes().getGUID();
        Term gotTerm = getTermByGUID(guid);
        FVTUtils.validateNode(gotTerm);
        System.out.println("Update term1");
        Term updatedTerm = updateTerm(guid, termForUpdate);
        FVTUtils.validateNode(updatedTerm);
        System.out.println("Get term1 again");
        gotTerm = getTermByGUID(guid);
        FVTUtils.validateNode(gotTerm);
        System.out.println("Delete term1");
        deleteTerm(guid);
        System.out.println("Restore term1");
        //FVTUtils.validateNode(gotTerm);
        gotTerm = restoreTerm(guid);
        FVTUtils.validateNode(gotTerm);
        System.out.println("Delete term1 again");
        deleteTerm(guid);
        //FVTUtils.validateNode(gotTerm);
        System.out.println("Purge term1");
        purgeTerm(guid);
        System.out.println("Create term3 with governance actions");
        GovernanceActions governanceActions = createGovernanceActions();
        Term term3 = createTermWithGovernanceActions(DEFAULT_TEST_TERM_NAME, glossaryGuid,governanceActions);
        FVTUtils.validateNode(term3);
        if (!governanceActions.getConfidence().getLevel().equals(term3.getGovernanceActions().getConfidence().getLevel())){
            throw new SubjectAreaFVTCheckedException("ERROR: Governance actions confidence not returned  as expected");
        }
        if (!governanceActions.getConfidentiality().getLevel().equals(term3.getGovernanceActions().getConfidentiality().getLevel())) {
            throw new SubjectAreaFVTCheckedException("ERROR: Governance actions confidentiality not returned  as expected");
        }
        if (!governanceActions.getRetention().getBasis().equals(term3.getGovernanceActions().getRetention().getBasis())) {
            throw new SubjectAreaFVTCheckedException("ERROR: Governance actions retention not returned  as expected");
        }
        if (!governanceActions.getCriticality().getLevel().equals(term3.getGovernanceActions().getCriticality().getLevel())) {
            throw new SubjectAreaFVTCheckedException("ERROR: Governance actions criticality not returned  as expected. ");
        }
        GovernanceActions governanceActions2 = create2ndGovernanceActions();
        System.out.println("Update term3 with and change governance actions");
        Term term3ForUpdate = new Term();
        term3ForUpdate.setName(DEFAULT_TEST_TERM_NAME_UPDATED);
        term3ForUpdate.setGovernanceActions(governanceActions2);

        Term updatedTerm3 = updateTerm(term3.getSystemAttributes().getGUID(), term3ForUpdate);
        FVTUtils.validateNode(updatedTerm3);
        if (!governanceActions2.getConfidence().getLevel().equals(updatedTerm3.getGovernanceActions().getConfidence().getLevel())){
            throw new SubjectAreaFVTCheckedException("ERROR: Governance actions confidence not returned  as expected");
        }
        if (!governanceActions2.getConfidentiality().getLevel().equals(updatedTerm3.getGovernanceActions().getConfidentiality().getLevel())) {
            throw new SubjectAreaFVTCheckedException("ERROR: Governance actions confidentiality not returned  as expected");
        }
        if (updatedTerm3.getGovernanceActions().getRetention() !=null) {
            throw new SubjectAreaFVTCheckedException("ERROR: Governance actions retention not null as expected");
        }
        // https://github.com/odpi/egeria/issues/3457  the below line when uncommented causes an error with the graph repo.
//        if (updatedTerm3.getGovernanceActions().getCriticality().getLevel() !=null) {
//            throw new SubjectAreaFVTCheckedException("ERROR: Governance actions criticality not returned as expected. It is " + updatedTerm3.getGovernanceActions().getCriticality().getLevel().getName());
//        }
        String spacedTermName = "This is a Term with spaces in name";
        int allcount  = subjectAreaTerm.findAll(userId).size();
        int yyycount = findTerms("yyy").size();
        int zzzcount = findTerms("zzz").size();
        int spacedTermcount = findTerms( spacedTermName).size();

        System.out.println("create terms to find");
        Term termForFind1 = getTermForInput("abc",glossaryGuid);
        termForFind1.setQualifiedName("yyy");
        termForFind1 = issueCreateTerm(termForFind1);
        FVTUtils.validateNode(termForFind1);
        Term termForFind2 = createTerm("yyy",glossaryGuid);
        FVTUtils.validateNode(termForFind2);
        Term termForFind3 = createTerm("zzz",glossaryGuid);
        FVTUtils.validateNode(termForFind3);
        Term termForFind4 = createTerm("This is a Term with spaces in name",glossaryGuid);
        FVTUtils.validateNode(termForFind4);

        results = findTerms("zzz");
        if (results.size() !=zzzcount+1 ) {
            throw new SubjectAreaFVTCheckedException("ERROR: zzz Expected " + zzzcount+1+ " back on the find got " +results.size());
        }
        results = findTerms("yyy");
        if (results.size() !=yyycount + 2) {
            throw new SubjectAreaFVTCheckedException("ERROR: yyy Expected " + yyycount+1 + " back on the find got " +results.size());
        }
        results = findTerms(null); //it's find all terms
        if (results.size() !=allcount + 4 ) {
            throw new SubjectAreaFVTCheckedException("ERROR: allcount Expected " + allcount + 4 + " back on the find got " +results.size());
        }

        results = subjectAreaTerm.findAll(userId); //it's find all terms
        if (results.size() !=allcount + 4 ) {
            throw new SubjectAreaFVTCheckedException("ERROR: allcount2 Expected " + allcount + 4 + " back on the find got " +results.size());
        }
        //soft delete a term and check it is not found
        deleteTerm(termForFind2.getSystemAttributes().getGUID());
        //FVTUtils.validateNode(deleted4);
        results = findTerms("yyy");
        if (results.size() !=yyycount +1 ) {
            throw new SubjectAreaFVTCheckedException("ERROR: yyy2 Expected " +yyycount +1  + " back on the find got " +results.size());
        }

       // search for a term with a name with spaces in
        results = findTerms(spacedTermName);
        if (results.size() != spacedTermcount +1 ) {
            throw new SubjectAreaFVTCheckedException("ERROR: Expected spaced " + spacedTermcount+1 + " back on the find got "  +results.size());
        }
        Term term = results.get(0);
        long now = new Date().getTime();
        Date fromTermTime = new Date(now+6*1000*60*60*24);
        Date toTermTime = new Date(now+7*1000*60*60*24);

        term.setEffectiveFromTime(fromTermTime);
        term.setEffectiveToTime(toTermTime);
        Term updatedFutureTerm = updateTerm(term.getSystemAttributes().getGUID(), term);
        if (updatedFutureTerm.getEffectiveFromTime().getTime()!=fromTermTime.getTime()) {
            throw new SubjectAreaFVTCheckedException("ERROR: Expected term from time to update");
        }
        if (updatedFutureTerm.getEffectiveToTime().getTime() !=toTermTime.getTime()) {
            throw new SubjectAreaFVTCheckedException("ERROR: Expected term to time to update");
        }
        Date fromGlossaryTime = new Date(now+8*1000*60*60*24);
        Date toGlossaryTime = new Date(now+9*1000*60*60*24);
        glossary.setEffectiveFromTime(fromGlossaryTime);
        glossary.setEffectiveToTime(toGlossaryTime);
        Glossary updatedFutureGlossary= glossaryFVT.updateGlossary(glossaryGuid, glossary);

        if (updatedFutureGlossary.getEffectiveFromTime().getTime()!= fromGlossaryTime.getTime()) {
            throw new SubjectAreaFVTCheckedException("ERROR: Expected glossary from time to update");
        }
        if (updatedFutureGlossary.getEffectiveToTime().getTime()!= toGlossaryTime.getTime()) {
            throw new SubjectAreaFVTCheckedException("ERROR: Expected glossary to time to update");
        }

        Term newTerm = getTermByGUID(term.getSystemAttributes().getGUID());

        GlossarySummary glossarySummary =  newTerm.getGlossary();

        if (glossarySummary.getFromEffectivityTime().getTime()!= fromGlossaryTime.getTime()) {
            throw new SubjectAreaFVTCheckedException("ERROR: Expected from glossary summary time "+glossarySummary.getFromEffectivityTime().getTime()+ " to equal " +fromGlossaryTime.getTime());
        }
        if (glossarySummary.getToEffectivityTime().getTime()!= toGlossaryTime.getTime()) {
            throw new SubjectAreaFVTCheckedException("ERROR: Expected to glossary summary time "+glossarySummary.getToEffectivityTime().getTime()+ " to equal " +toGlossaryTime.getTime());
        }

        if (glossarySummary.getRelationshipguid() == null) {
            throw new SubjectAreaFVTCheckedException("ERROR: Expected glossary summary non null relationship");
        }
        if (glossarySummary.getFromRelationshipEffectivityTime() != null) {
            throw new SubjectAreaFVTCheckedException("ERROR: Expected glossary summary null relationship from time");
        }
        if (glossarySummary.getToRelationshipEffectivityTime() != null) {
            throw new SubjectAreaFVTCheckedException("ERROR: Expected glossary summary null relationship to time");
        }
        Term term5 = new Term();
        term5.setSpineObject(true);
        term5.setName("Term5");
        glossarySummary = new GlossarySummary();
        glossarySummary.setGuid(glossaryGuid);
        term5.setGlossary(glossarySummary);
        Term createdTerm5 = issueCreateTerm(term5);
        if (createdTerm5.isSpineObject() == false) {
            throw new SubjectAreaFVTCheckedException("ERROR: Expected isSpineObject to be true ");
        }
        Term term6 = new Term();
        term6.setSpineAttribute(true);
        term6.setName("Term6");
        glossarySummary = new GlossarySummary();
        glossarySummary.setGuid(glossaryGuid);
        term6.setGlossary(glossarySummary);
        Term createdTerm6 = issueCreateTerm(term6);
        if (createdTerm6.isSpineAttribute() == false) {
            throw new SubjectAreaFVTCheckedException("ERROR: Expected isSpineAttribute to be true ");
        }
        Term term7 = new Term();
        term7.setObjectIdentifier(true);
        term7.setName("Term7");
        glossarySummary = new GlossarySummary();
        glossarySummary.setGuid(glossaryGuid);
        term7.setGlossary(glossarySummary);
        Term createdTerm7 = issueCreateTerm(term7);
        if (createdTerm7.isObjectIdentifier() == false) {
            throw new SubjectAreaFVTCheckedException("ERROR: Expected isObjectIdentifier to be true ");
        }
        // make sure there is a term with the name
         createTerm(DEFAULT_TEST_TERM_NAME, glossaryGuid);

        Term termForUniqueQFN2= createTerm(DEFAULT_TEST_TERM_NAME, glossaryGuid);
        if (termForUniqueQFN2 == null || termForUniqueQFN2.equals("")) {
            throw new SubjectAreaFVTCheckedException("ERROR: Expected qualified name to be set");
        }

        // test categories

        Category cat1 = categoryFVT.createCategoryWithGlossaryGuid("cat1", glossaryGuid);
        Category cat2 = subjectAreaFVT.createSubjectAreaDefinitionWithGlossaryGuid("cat2", glossaryGuid);
        Category cat3 = categoryFVT.createCategoryWithGlossaryGuid("cat3",glossaryGuid);
        CategorySummary cat1Summary = new CategorySummary();
        cat1Summary.setGuid(cat1.getSystemAttributes().getGUID());
        CategorySummary cat2Summary = new CategorySummary();
        cat2Summary.setGuid(cat2.getSystemAttributes().getGUID());
        CategorySummary cat3Summary = new CategorySummary();
        cat3Summary.setGuid(cat3.getSystemAttributes().getGUID());

        List<CategorySummary> suppliedCategories = new ArrayList<>();
        suppliedCategories.add(cat1Summary);

        Term term4cats = getTermForInput(DEFAULT_TEST_TERM_NAME,glossaryGuid);
        Term createdTerm4cats =issueCreateTerm(term4cats);
        if (createdTerm4cats.getCategories() != null) {
            throw new SubjectAreaFVTCheckedException("ERROR: Expected null categories created when none were requested");
        }

        term4cats = getTermForInput(DEFAULT_TEST_TERM_NAME,glossaryGuid);
        term4cats.setCategories(suppliedCategories);
        createdTerm4cats =issueCreateTerm(term4cats);
        if (createdTerm4cats.getCategories().size() != 1) {
            throw new SubjectAreaFVTCheckedException("ERROR: Expected 1 categories returned");
        }
        if (!createdTerm4cats.getCategories().get(0).getGuid().equals(cat1Summary.getGuid())) {
            throw new SubjectAreaFVTCheckedException("ERROR: Expected response category guid to match the requested category guid.");
        }
        if (categoryFVT.getTerms(cat1.getSystemAttributes().getGUID()).size() != 1) {
            throw new SubjectAreaFVTCheckedException("ERROR: Expected the category to have 1 term.");
        }

        suppliedCategories.add(cat2Summary);
        term4cats.setCategories(suppliedCategories);
        Term createdTerm4cats2 =issueCreateTerm(term4cats);
        if (createdTerm4cats2.getCategories().size() != 2) {
            throw new SubjectAreaFVTCheckedException("ERROR: Expected 2 categories returned");
        }
        List<Category> categories = getCategoriesAPI(createdTerm4cats2.getSystemAttributes().getGUID(),0,5);
        if (categories.size() !=2) {
            throw new SubjectAreaFVTCheckedException("ERROR: Expected 2 categories returned on get Categories API call");
        }

        // update with null categories should change nothing
        createdTerm4cats2.setCategories(null);
        Term updatedTerm4cats2 = updateTerm(createdTerm4cats2.getSystemAttributes().getGUID(),createdTerm4cats2);
        if (updatedTerm4cats2.getCategories().size() != 2) {
            throw new SubjectAreaFVTCheckedException("ERROR: Expected 2 categories returned");
        }
        if (getCategoriesAPI(updatedTerm4cats2.getSystemAttributes().getGUID(),0,5).size() !=2) {
            throw new SubjectAreaFVTCheckedException("ERROR: Expected 2 categories returned on get Categories API call after update");
        }
        if (getCategoriesAPI(updatedTerm4cats2.getSystemAttributes().getGUID(),1,5).size() !=1) {
            throw new SubjectAreaFVTCheckedException("ERROR: Expected 1 categories returned on get Categories API call after update startingFrom 1");
        }
        if (getCategoriesAPI(updatedTerm4cats2.getSystemAttributes().getGUID(),0,1).size() !=1) {
            throw new SubjectAreaFVTCheckedException("ERROR: Expected 1 categories returned on get Categories API call after update pageSize 1");
        }

        // replace categories with null
        createdTerm4cats.setCategories(null);
        Term replacedTerm4cats = replaceTerm(createdTerm4cats.getSystemAttributes().getGUID(), createdTerm4cats);
        if (replacedTerm4cats.getCategories() != null) {
            throw new SubjectAreaFVTCheckedException("ERROR: Expected replace with null to get rid of the categorizations.");
        }
        List<Category> cats = getCategoriesAPI(replacedTerm4cats.getSystemAttributes().getGUID(),0,5);
        if (cats ==null || cats.size() != 0) {
            throw new SubjectAreaFVTCheckedException("ERROR: Use API call to check replace with null to get rid of the categorizations.");
        }
        // update term to gain 2 categories
        createdTerm4cats.setCategories(suppliedCategories);
        updatedTerm4cats2 = updateTerm(createdTerm4cats.getSystemAttributes().getGUID(),createdTerm4cats);
        if (updatedTerm4cats2.getCategories().size() != 2) {
            throw new SubjectAreaFVTCheckedException("ERROR: Expected update to gain 2 categorizations.");
        }
        if (getCategoriesAPI(updatedTerm4cats2.getSystemAttributes().getGUID(),0,5).size() !=2) {
            throw new SubjectAreaFVTCheckedException("ERROR: Use API call to check update to gain 2 categorizations");
        }

        List<CategorySummary> supplied3Categories = new ArrayList<>();
        supplied3Categories.add(cat1Summary);
        supplied3Categories.add(cat2Summary);
        supplied3Categories.add(cat3Summary);
        updatedTerm4cats2.setCategories(supplied3Categories);
        updatedTerm4cats2 = updateTerm(createdTerm4cats.getSystemAttributes().getGUID(), updatedTerm4cats2);
        if (updatedTerm4cats2.getCategories().size() != 3) {
            throw new SubjectAreaFVTCheckedException("ERROR: Expected update to have 3 categorizations.");
        }

        // clean up
        categoryFVT.deleteCategory(cat1Summary.getGuid());
        categoryFVT.purgeCategory(cat1Summary.getGuid());
        categoryFVT.deleteCategory(cat2Summary.getGuid());
        categoryFVT.purgeCategory(cat2Summary.getGuid());
        categoryFVT.deleteCategory(cat3Summary.getGuid());
        categoryFVT.purgeCategory(cat3Summary.getGuid());
        deleteTerm(createdTerm4cats.getSystemAttributes().getGUID());
        purgeTerm(createdTerm4cats.getSystemAttributes().getGUID());
        deleteTerm(createdTerm4cats2.getSystemAttributes().getGUID());
        purgeTerm(createdTerm4cats2.getSystemAttributes().getGUID());

    }

    public  Term createTerm(String termName, String glossaryGuid) throws InvalidParameterException, PropertyServerException, UserNotAuthorizedException {
        Term term = getTermForInput(termName, glossaryGuid);
        return issueCreateTerm(term);
    }

    public Term issueCreateTerm(Term term) throws InvalidParameterException, PropertyServerException, UserNotAuthorizedException {
        Term newTerm = subjectAreaTerm.create(this.userId, term);
        if (newTerm != null)
        {
            String guid = newTerm.getSystemAttributes().getGUID();
            System.out.println("Created Term " + newTerm.getName() + " with guid " + guid);
            createdTermsSet.add(guid);
        }
        return newTerm;
    }

    private Term getTermForInput(String termName, String glossaryGuid) {
        Term term = new Term();
        term.setName(termName);
        GlossarySummary glossarySummary = new GlossarySummary();
        glossarySummary.setGuid(glossaryGuid);
        term.setGlossary(glossarySummary);
        return term;
    }

    public  Term createTermWithGovernanceActions(String termName, String glossaryGuid,GovernanceActions governanceActions) throws InvalidParameterException, PropertyServerException, UserNotAuthorizedException {
        Term term = getTermForInput(termName, glossaryGuid);
        term.setGovernanceActions(governanceActions);
        Term newTerm = issueCreateTerm(term);
        return newTerm;
    }

    private GovernanceActions createGovernanceActions() {
        GovernanceActions governanceActions = new GovernanceActions();
        Confidentiality confidentiality = new Confidentiality();
        confidentiality.setLevel(6);
        governanceActions.setConfidentiality(confidentiality);

        Confidence confidence = new Confidence();
        confidence.setLevel(ConfidenceLevel.Authoritative);
        governanceActions.setConfidence(confidence);

        Criticality criticality = new Criticality();
        criticality.setLevel(CriticalityLevel.Catastrophic);
        governanceActions.setCriticality(criticality);

        Retention retention = new Retention();
        retention.setBasis(RetentionBasis.ProjectLifetime);
        governanceActions.setRetention(retention);
        return governanceActions;
    }
    private GovernanceActions create2ndGovernanceActions() {
        GovernanceActions governanceActions = new GovernanceActions();
        Confidentiality confidentiality = new Confidentiality();
        confidentiality.setLevel(5);
        governanceActions.setConfidentiality(confidentiality);

        Confidence confidence = new Confidence();
        confidence.setLevel(ConfidenceLevel.AdHoc);
        governanceActions.setConfidence(confidence);
        // remove this classification level
        Criticality criticality = new Criticality();
        criticality.setLevel(null);
        governanceActions.setCriticality(criticality);
        // remove retention by nulling it
        governanceActions.setRetention(null);
        return governanceActions;
    }


    public Term getTermByGUID(String guid) throws InvalidParameterException, PropertyServerException, UserNotAuthorizedException {
        Term term = subjectAreaTerm.getByGUID(this.userId, guid);
        if (term != null)
        {
            System.out.println("Got Term " + term.getName() + " with userId " + term.getSystemAttributes().getGUID() + " and status " + term.getSystemAttributes().getStatus());
        }
        return term;
    }
    public List<Term> findTerms(String criteria) throws InvalidParameterException, PropertyServerException, UserNotAuthorizedException {
        FindRequest findRequest = new FindRequest();
        findRequest.setSearchCriteria(criteria);
        List<Term> terms = subjectAreaTerm.find(this.userId, findRequest);
        return terms;
    }

    public Term updateTerm(String guid, Term term) throws InvalidParameterException, PropertyServerException, UserNotAuthorizedException {
        Term updatedTerm = subjectAreaTerm.update(this.userId, guid, term);
        if (updatedTerm != null)
        {
            System.out.println("Updated Term name to " + updatedTerm.getName());
        }
        return updatedTerm;
    }
    public Term replaceTerm(String guid, Term term) throws InvalidParameterException, PropertyServerException, UserNotAuthorizedException {
        Term updatedTerm = subjectAreaTerm.replace(this.userId, guid, term);
        if (updatedTerm != null)
        {
            System.out.println("Replaced Term name to " + updatedTerm.getName());
        }
        return updatedTerm;
    }
    public Term restoreTerm(String guid) throws InvalidParameterException, PropertyServerException, UserNotAuthorizedException {
        Term restoredTerm = subjectAreaTerm.restore(this.userId, guid);
        if (restoredTerm != null)
        {
            System.out.println("Restored Term " + restoredTerm.getName());
            createdTermsSet.add(guid);
        }
        return restoredTerm;
    }
    public Term updateTermToFuture(String guid, Term term) throws InvalidParameterException, PropertyServerException, UserNotAuthorizedException {
        long now = new Date().getTime();

       term.setEffectiveFromTime(new Date(now+6*1000*60*60*24));
       term.setEffectiveToTime(new Date(now+7*1000*60*60*24));

        Term updatedTerm = subjectAreaTerm.update(this.userId, guid, term);
        if (updatedTerm != null)
        {
            System.out.println("Updated Term name to " + updatedTerm.getName());
        }
        return updatedTerm;
    }

    public void deleteTerm(String guid) throws InvalidParameterException, PropertyServerException, UserNotAuthorizedException {
            subjectAreaTerm.delete(this.userId, guid);
            createdTermsSet.remove(guid);
            System.out.println("Delete succeeded");
    }

    /**
     * Purge - we should not need to decrement the createdTermsSet as the soft delete should have done this
     * @param guid
     * @throws InvalidParameterException
     * @throws PropertyServerException
     * @throws UserNotAuthorizedException
     */
    public void purgeTerm(String guid) throws InvalidParameterException, PropertyServerException, UserNotAuthorizedException {
        subjectAreaTerm.purge(this.userId, guid);
        System.out.println("Purge succeeded");
    }

    public List<Line> getTermRelationships(Term term) throws InvalidParameterException, PropertyServerException, UserNotAuthorizedException {
        return subjectAreaTerm.getAllRelationships(this.userId, term.getSystemAttributes().getGUID());
    }

    public List<Line> getTermRelationships(Term term, Date asOfTime, int offset, int pageSize, SequencingOrder sequenceOrder, String sequenceProperty) throws InvalidParameterException, PropertyServerException, UserNotAuthorizedException {
        FindRequest findRequest = new FindRequest();
        findRequest.setAsOfTime(asOfTime);
        findRequest.setStartingFrom(offset);
        findRequest.setPageSize(pageSize);
        findRequest.setSequencingOrder(sequenceOrder);
        findRequest.setSequencingProperty(sequenceProperty);
        return subjectAreaTerm.getRelationships(this.userId, term.getSystemAttributes().getGUID(),findRequest);
    }
    void deleteRemaining() throws UserNotAuthorizedException, PropertyServerException, InvalidParameterException, SubjectAreaFVTCheckedException {
        deleteRemainingTerms();
        glossaryFVT.deleteRemainingGlossaries();
    }
    void deleteRemainingTerms() throws InvalidParameterException, PropertyServerException, UserNotAuthorizedException, SubjectAreaFVTCheckedException {
        Iterator<String> iter =  createdTermsSet.iterator();
        while (iter.hasNext()) {
            String guid = iter.next();
            iter.remove();
            deleteTerm(guid);
        }
        List<Term> terms = findTerms(".*");
        if (terms.size() != existingTermCount) {
            throw new SubjectAreaFVTCheckedException("ERROR: Expected " +existingTermCount + " Terms to be found, got " + terms.size());
        }
    }
    public List<Category> getCategoriesAPI(String termGuid,int startingFrom, int pageSize) throws InvalidParameterException, PropertyServerException, UserNotAuthorizedException {
        FindRequest findRequest = new FindRequest();
        findRequest.setPageSize(pageSize);
        findRequest.setStartingFrom(startingFrom);
        return subjectAreaTermClient.getCategories(userId, termGuid, findRequest);
    }
}
