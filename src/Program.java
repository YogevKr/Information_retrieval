import java.io.File;
import java.io.IOException;
import java.util.*;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

public class Program {

    private static File m_DocsFile;
    private static File m_QueryFile;
    private static File m_OutputFile;

    private static String m_WorkingDir;
    private static String m_RetrievalAlgorithm = "";


    public static void main(String[] args) throws IOException, ParseException {

        if (args.length != 1){
            System.out.println("Software except exactly one parameter");
            System.exit(1);
        }

        initFromParameterFile(args[0]);

        SearchEngine searchEngine = new SearchEngine();
        searchEngine.SetRetrievalAlgorithm(m_RetrievalAlgorithm);
        searchEngine.SetStopWords();
        searchEngine.SetAnalyzer();
        searchEngine.SetIndex();
        searchEngine.AddDocsFile(m_DocsFile);
        searchEngine.GetScoreDocsForQuery("KENNEDY");



//        Fields fields = MultiFields.getFields(reader);
//        if (fields != null) {
//            Terms terms = fields.terms("docContent");
//            if (terms != null) {
//                TermsEnum termsEnum = terms.iterator();
//                BytesRef t = termsEnum.next();
//
//                while (t != null){
//                    System.out.println(termsEnum.term().toString());
//
//                    t = termsEnum.next();
//                }
//            }
//        }


        // 2. query
        String querystr = "KENNEDY";

        // the "title" arg specifies the default field to use
        // when no field is explicitly specified in the query.

        Query q = new QueryParser("docContent", searchEngine.m_Analyzer).parse(querystr);

        // 3. search
        int hitsPerPage = 10;
        IndexReader reader = DirectoryReader.open(searchEngine.m_Index);
        IndexSearcher searcher = new IndexSearcher(reader);
        TopDocs docs = searcher.search(q, hitsPerPage);
        ScoreDoc[] hits = docs.scoreDocs;

        // 4. display results
        System.out.println("Found " + hits.length + " hits.");
        for(int i=0;i<hits.length;++i) {
            int docId = hits[i].doc;
            Document d = searcher.doc(docId);
            System.out.println((i + 1) + ". " + d.get("docContent") + "\t" + d.get("docID"));
        }

        // reader can only be closed when there
        // is no need to access the documents any more.
        reader.close();
    }


    private static void initFromParameterFile(String parameterFilePath) {
        ArrayList<String> lines = Utils.fileToLineList(parameterFilePath);

        for (String line: lines) {

            if (line.startsWith("queryFile=")){
                m_QueryFile = new File(line.substring(line.indexOf('=') + 1));
            }
            else if ((line.startsWith("docsFile="))){
                m_DocsFile = new File(line.substring(line.indexOf('=') + 1));
            }
            else if ((line.startsWith("outputFile="))){
                m_OutputFile = new File(line.substring(line.indexOf('=') + 1));
            }
            else if ((line.startsWith("retrievalAlgorithm="))){
                m_RetrievalAlgorithm = line.substring(line.indexOf('=') + 1);
            }
        }

        if (!(m_RetrievalAlgorithm.equals("basic") || m_RetrievalAlgorithm.equals("improved"))){
            System.out.println("Invalid Retrieval Algorithm!");
            System.exit(1);
        }
    }


}