import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.misc.HighFreqTerms;
import org.apache.lucene.misc.TermStats;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchEngine {

    private Double m_Threshold;
    private String m_RetrievalAlgorithm = "";
    private ArrayList<String> m_StopWordList;
    private StandardAnalyzer m_Analyzer;
    private Directory m_Index;
    private IndexWriterConfig m_IndexWriterConfig;
    private Similarity m_SimilarityMethod;


    public void SetThreshold(Double i_t) {
        m_Threshold = i_t;
    }

    public void AddDocsFile(File i_DocsFile) throws IOException {

        ArrayList<String> docsFileLinesRaw = Utils.fileToLineList(i_DocsFile);
        Map<String, String> docs = new HashMap<>();
        StringBuilder doc = new StringBuilder();
        String key = "";

        for (String line : docsFileLinesRaw) {
            if (line.startsWith("*TEXT ")) {
                if (!key.equals("")) {
                    docs.put(key, doc.toString());
                }

                doc = new StringBuilder();
                key = line;
            } else {
                doc.append(" ");
                doc.append(line);
            }
        }

        if (!key.equals("")) {
            docs.put(key, doc.toString());
        }

        IndexWriter w = new IndexWriter(m_Index, m_IndexWriterConfig);
        Pattern pattern = Pattern.compile("\\*TEXT (\\d+)");

        for (Map.Entry<String, String> entry : docs.entrySet()) {
            Matcher matcher = pattern.matcher(entry.getKey());
            matcher.find();
            addDoc(w, matcher.group(1), entry.getValue());
        }

        w.close();
    }

    public void InitStopWords() {
        m_StopWordList = new ArrayList<>();
    }

    public void SetStopWords(ArrayList<String> i_termList) {
        m_StopWordList = i_termList;
    }

    public ArrayList<String> GetMostCommonTerms(int i_n) throws Exception {
        ArrayList<String> termList = new ArrayList<>();
        IndexReader reader = DirectoryReader.open(m_Index);
        TermStats[] terms = HighFreqTerms.getHighFreqTerms(reader, i_n,
                "docContent", new HighFreqTerms.DocFreqComparator());

        for (TermStats term : terms) {
            termList.add(term.termtext.utf8ToString());
        }

        reader.close();

        return termList;
    }

    public Map<String, Float> GetScoreDocsForQuery(String i_QueryStr) throws IOException, ParseException {

        int hitsPerPage = 20;

        IndexReader reader = DirectoryReader.open(m_Index);
        IndexSearcher searcher = new IndexSearcher(reader);
        searcher.setSimilarity(m_SimilarityMethod);

        Query query = new QueryParser("docContent", m_Analyzer).parse(i_QueryStr);

        TopDocs docs = searcher.search(query, hitsPerPage);

        Map<String, Float> docsMap = new HashMap<>();

        for (ScoreDoc doc : docs.scoreDocs) {
            Document d = searcher.doc(doc.doc);
            String id = d.get("docID");
            docsMap.put(id, doc.score);


            if (doc.score < m_Threshold)
                break;
        }

        reader.close();

        return docsMap;
    }

    public ArrayList<String> GetQueriesFromFile(File i_QueryFile) {
        ArrayList<String> queryFileLinesRaw = Utils.fileToLineList(i_QueryFile);
        ArrayList<String> queries = new ArrayList<>();
        StringBuilder query = null;

        for (String line : queryFileLinesRaw) {
            if (line.startsWith("*FIND ")) {

                if (query != null) {
                    queries.add(query.toString());
                }

                query = new StringBuilder();
            } else {

                if (query != null) {
                    query.append(" ");
                    query.append(line);
                }
            }
        }

        if (query != null && !query.toString().equals("")) {
            queries.add(query.toString());
        }

        return queries;
    }

    public void SetAnalyzer() {
        //    Specify the analyzer for tokenizing text.
        //    The same analyzer should be used for indexing and searching

        m_Analyzer = new StandardAnalyzer(StopFilter.makeStopSet(m_StopWordList));
    }

    public void SetIndex() {
        m_Index = new RAMDirectory();
        m_IndexWriterConfig = new IndexWriterConfig(m_Analyzer);
    }

    public void SetRetrievalAlgorithm(String i_RetrievalAlgorithm) {
        m_RetrievalAlgorithm = i_RetrievalAlgorithm;

        if (i_RetrievalAlgorithm.equals("basic")) {
            m_Threshold = 4.9572;
            m_SimilarityMethod = new SimilarityBase() {
                @Override
                protected float score(BasicStats i_basicStats, float i_tf, float i_docLen) {
                    long N = i_basicStats.getNumberOfDocuments();
                    double DFt = i_basicStats.getDocFreq() + 1;

                    double idf = Math.log10(N / DFt);

                    return (float) (1 + Math.log10(i_tf) * idf);
                }

                @Override
                public String toString() {
                    return "TF-IDF";
                }
            };
        } else if (i_RetrievalAlgorithm.equals("improved")) {

            // https://lucene.apache.org/core/7_0_0/core/org/apache/lucene/search/similarities/TFIDFSimilarity.html#TFIDFSimilarity--
            m_Threshold = 1.365;

            m_SimilarityMethod = new TFIDFSimilarity() {
                @Override
                public float tf(float i_Freq) {
                    return (float) Math.sqrt(i_Freq);
                }

                @Override
                public float idf(long i_DocFreq, long i_DocCount) {
                    return (float) (Math.log((i_DocCount + 1.0) / (i_DocFreq + 1.0)) + 1.0);
                }

                @Override
                public float lengthNorm(int i_NumTerms) {
                    return (float) (1.0 / Math.sqrt(i_NumTerms));
                }

                @Override
                public float sloppyFreq(int i_Distance) {
                    return 1.0F / (float) (i_Distance + 1);
                }

                @Override
                public float scorePayload(int doc, int start, int end, BytesRef payload) {
                    return 1.0F;
                }
            };
        }
    }

    private void addDoc(IndexWriter i_w, String i_id, String i_docContent) throws IOException {
        Document doc = new Document();

        doc.add(new TextField("docContent", i_docContent, Field.Store.YES));
        doc.add(new StringField("docID", i_id, Field.Store.YES));

        i_w.addDocument(doc);
    }


}
