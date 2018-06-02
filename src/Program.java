import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;


public class Program {

    private static final int THRESHOLD = 12;
    private static File m_DocsFile;
    private static File m_QueryFile;
    private static FileWriter m_OutputFile;

    private static String m_WorkingDir;
    private static String m_RetrievalAlgorithm = "";
    private static SearchEngine m_SearchEngine;


    public static void main(String[] args) throws IOException, ParseException {

        if (args.length != 1){
            System.out.println("Software except exactly one parameter");
            System.exit(1);
        }

        initFromParameterFile(args[0]);

        m_SearchEngine = new SearchEngine();
        m_SearchEngine.SetRetrievalAlgorithm(m_RetrievalAlgorithm);
        m_SearchEngine.SetThreshold(THRESHOLD);
        m_SearchEngine.SetStopWords();
        m_SearchEngine.SetAnalyzer();
        m_SearchEngine.SetIndex();
        m_SearchEngine.AddDocsFile(m_DocsFile);

        executeAllQueries();

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

    }

    private static void executeAllQueries() throws IOException, ParseException {
        ArrayList<String> queries = m_SearchEngine.GetQueriesFromFile(m_QueryFile);

        for (int i = 0; i < queries.size(); i++){
            ScoreDoc[] scoreDocs = m_SearchEngine.GetScoreDocsForQuery(queries.get(i));
            ArrayList<Integer> docsList = m_SearchEngine.
                    GetDocsIdListFromScoreDocsWithThreshold(scoreDocs);
            Collections.sort(docsList);

            StringBuilder docsId = new StringBuilder();

            for (int docId: docsList){
                docsId.append(docId + " ");
            }

            m_OutputFile.write((i + 1) + " " + docsId.toString() + "\n");
        }

        m_OutputFile.close();
    }

    private static void initFromParameterFile(String i_parameterFilePath) {
        ArrayList<String> lines = Utils.fileToLineList(i_parameterFilePath);

        for (String line: lines) {

            if (line.startsWith("queryFile=")){
                m_QueryFile = new File(line.substring(line.indexOf('=') + 1));
            }
            else if ((line.startsWith("docsFile="))){
                m_DocsFile = new File(line.substring(line.indexOf('=') + 1));
            }
            else if ((line.startsWith("outputFile="))){
                try {
                    m_OutputFile = new FileWriter(line.substring(line.indexOf('=') + 1));
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
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