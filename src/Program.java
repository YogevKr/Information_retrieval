import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;
import sun.awt.SunHints;


public class Program {

    private static final int THRESHOLD = 12;
    private static File m_DocsFile;
    private static File m_QueryFile;
    private static FileWriter m_OutputFile;

    private static String m_WorkingDir;
    private static String m_RetrievalAlgorithm = "";
    private static SearchEngine m_SearchEngine;
    private static Map<Integer, Map> m_QueriesResults;
    private static Map<Integer, String[]> m_Truth;


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
        writeQueriesResultsToFile();
        parseTheTruth("truth.txt");

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

    private static void parseTheTruth(String i_PathToTheTruth){
        ArrayList<String> lines = Utils.fileToLineList(i_PathToTheTruth);
        Map<Integer, String[]> truth = new HashMap<>();

        for (String line : lines){
            if (!line.equals("")) {
                String[] sp = line.split(" +",2);
                truth.put(Integer.parseInt(sp[0]), sp[1].split(" +"));
            }
        }
        m_Truth = truth;
    }

    private static void executeAllQueries() throws IOException, ParseException {
        ArrayList<String> queries = m_SearchEngine.GetQueriesFromFile(m_QueryFile);
        Map<Integer, Map> queriesResults = new HashMap<>();

        for (int i = 0; i < queries.size(); i++) {
            Map<String, Float> scoreDocs = m_SearchEngine.GetScoreDocsForQuery(queries.get(i));
            queriesResults.put((i + 1), scoreDocs);

            m_QueriesResults = queriesResults;
        }
    }

    private static void writeQueriesResultsToFile() throws IOException {

        ArrayList<Integer> queriesIds = new ArrayList<>(m_QueriesResults.keySet());
        Collections.sort(queriesIds);

        for (int id: queriesIds) {
            ArrayList<String> docsList = new ArrayList<String>(m_QueriesResults.get(id).keySet());
            Collections.sort(docsList);

            StringBuilder docsId = new StringBuilder();

            for (String docId: docsList){
                docsId.append(docId + " ");
            }

            m_OutputFile.write(id + " " + docsId.toString() + "\n");
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