import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.misc.TermStats;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.misc.HighFreqTerms;
import org.apache.lucene.misc.HighFreqTerms.TotalTermFreqComparator;
import org.apache.lucene.util.BytesRef;


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
        searchEngine.SetStopWords();
        searchEngine.SetAnalayzer();
        searchEngine.SetIndex();



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
        String querystr = args.length > 0 ? args[0] : "lucene";

        // the "title" arg specifies the default field to use
        // when no field is explicitly specified in the query.

        Query q = new QueryParser("title", searchEngine.m_Analyzer).parse(querystr);

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
            System.out.println((i + 1) + ". " + d.get("isbn") + "\t" + d.get("title"));
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