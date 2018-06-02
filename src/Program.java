import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import com.sun.corba.se.impl.oa.toa.TOA;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;
import sun.awt.SunHints;


public class Program {

    private static final int THRESHOLD = 12;
    private static final double ALPHA = 0.5;
    private static final double BETA = 1;

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
        runExperiment();

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

    private static void runExperiment() {

        for (int i = 1; i <= m_Truth.size(); i++){

            //TP
            ArrayList<String> predictionDocs = new ArrayList<>(m_QueriesResults.get(i).keySet());
            predictionDocs.retainAll(new ArrayList<String>(Arrays.asList(m_Truth.get(i))));
            int TP = predictionDocs.size();

            //FP
            predictionDocs = new ArrayList<>(m_QueriesResults.get(i).keySet());
            predictionDocs.removeAll(Arrays.asList(m_Truth.get(i)));
            int FP = predictionDocs.size();

            //FN
            predictionDocs = new ArrayList<>(m_QueriesResults.get(i).keySet());
            ArrayList<String> truth = new ArrayList<>(Arrays.asList(m_Truth.get(i)));
            truth.removeAll(predictionDocs);
            int FN = truth.size();

            double precision = (TP == 0) ? FP/(TP + 1.0) : FP/(TP + 0.0);
            double recall = (TP == 0) ? FN/(TP + 1.0) : FN/(TP + 0.0);

            // TODO: Check this formula
            double F = 1 / ((ALPHA / precision) + ((1 - ALPHA) / recall));

            System.out.println(String.format("%d Precision = %.2f Recall = %.2f F = %.2f", i, precision, recall, F));
        }

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