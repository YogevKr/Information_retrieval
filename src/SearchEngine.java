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
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchEngine {

    private String m_RetrievalAlgorithm = "";
    private ArrayList<String> m_StopWordList;
    public StandardAnalyzer m_Analyzer; // TODO change to private
    public Directory m_Index; // TODO change to private
    private IndexWriterConfig m_IndexWriterConfig;

    public void AddDocsFile(File i_DocsFile) throws IOException {

        ArrayList<String> docsFileLinesRaw = Utils.fileToLineList(i_DocsFile);
        Map<String, String> docs = new HashMap<String, String>();
        StringBuilder doc = new StringBuilder();
        String key = "";

        for (String line: docsFileLinesRaw) {
            if (line.startsWith("*TEXT ")){
                if (!key.equals("")){
                    docs.put(key, doc.toString());
                }

                doc = new StringBuilder();
                key = line;
            }
            else{
                doc.append(" ");
                doc.append(line);
            }
        }

        IndexWriter w = new IndexWriter(m_Index, m_IndexWriterConfig);
        Pattern pattern = Pattern.compile("\\*TEXT (\\d?)");

        for (Map.Entry<String, String> entry : docs.entrySet()) {
            Matcher matcher = pattern.matcher(entry.getKey());
            matcher.find();
            addDoc(w, matcher.group(1), entry.getValue());
        }

        w.close();
    }

    //TODO
    public void SetStopWords(){
        m_StopWordList = new ArrayList<String>();
    }

    public void SetAnalayzer(){
        // 0. Specify the analyzer for tokenizing text.
        //    The same analyzer should be used for indexing and searching

        m_Analyzer = new StandardAnalyzer(StopFilter.makeStopSet(m_StopWordList));
    }

    public void SetIndex() throws IOException {
        m_Index = new RAMDirectory();
        m_IndexWriterConfig = new IndexWriterConfig(m_Analyzer);
    }

    private void addDoc(IndexWriter i_w, String i_id, String i_docContent) throws IOException {
        Document doc = new Document();

        doc.add(new TextField("docContent", i_docContent, Field.Store.YES));
        doc.add(new StringField("docID", i_id, Field.Store.YES));

        i_w.addDocument(doc);
    }
}
