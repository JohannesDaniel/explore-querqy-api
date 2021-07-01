package integration;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.json.JsonQueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import querqy.model.convert.converter.MapConverterConfig;
import querqy.rewrite.experimental.QueryRewritingHandler;
import querqy.rewrite.experimental.RewrittenQuery;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class ExternalRewritingTest {

    private SolrClient solrClient;
    private QueryRewritingHandler rewritingHandler;


    private static final String INDEX_NAME = "sample-index";

    private static final String ID_FIELD_NAME = "id";
    private static final String PRODUCT_TYPE_FIELD_NAME = "product_type";

    @Before
    public void prepareSolrClient() throws IOException, SolrServerException {
        solrClient = new HttpSolrClient.Builder("http://localhost:8983/solr/" + INDEX_NAME).build();

        solrClient.add(
                docs(
                        doc("1", "notebook"),
                        doc("2", "notebook"),
                        doc("3", "laptop"),
                        doc("4", "laptop"),
                        doc("5", "smartphone")
                )
        );

        solrClient.commit();
    }

    @Before
    public void prepareRewritingHandler() throws IOException {
        rewritingHandler = QueryRewritingHandler.builder()
                .addReplaceRewriter("notbook => notebook")
                .addCommonRulesRewriter(
                        "notebook => \n " +
                                "SYNONYM: laptop \n "
                )
                .build();
    }

    @Test
    public void externalRewriting_ForQueryTermWithSpelling_ReplacementAndSynonymActionsAreApplied() throws IOException, SolrServerException {
        final RewrittenQuery rewrittenQuery = rewritingHandler.rewriteQuery("notbook");
        final JsonQueryRequest request = new JsonQueryRequest();

        request
                .setQuery(createQuerqyQuery(rewrittenQuery))
                .withParam("qf", "product_type");

        final QueryResponse response = request.process(solrClient);
        final List<String> ids = transformSolrDocumentListToIdList(response.getResults());

        assertThat(ids).containsExactlyInAnyOrder("1", "2", "3", "4");
    }

    private static SolrInputDocument doc(final String id, final String productType) {
        final SolrInputDocument doc = new SolrInputDocument();

        doc.addField(ID_FIELD_NAME, id);
        doc.addField(PRODUCT_TYPE_FIELD_NAME, productType);

        return doc;
    }

    private static List<SolrInputDocument> docs(final SolrInputDocument... docs) {
        return Arrays.asList(docs);
    }

    public static Map<String, Object> createQuerqyQuery(final RewrittenQuery rewrittenQuery) {
        final Map expandedQuery = rewrittenQuery.getQuery().toMap(
                MapConverterConfig.builder()
                        .parseBooleanToString(true)
                        .build());

        return Collections.singletonMap("querqy", Collections.singletonMap("query", expandedQuery));
    }

    public static List<String> transformSolrDocumentListToIdList(final SolrDocumentList solrDocuments) {
        return solrDocuments.stream()
                .map(solrDocument -> solrDocument.get("id"))
                .map(id -> (String) id)
                .collect(Collectors.toList());
    }
}
