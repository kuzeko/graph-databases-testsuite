import com.bigdata.journal.IIndexManager;
import com.bigdata.journal.Journal;
import com.bigdata.journal.StoreTypeEnum;
import com.bigdata.rdf.model.BigdataStatement;
import com.bigdata.rdf.sail.BigdataSail;
import com.bigdata.rdf.sail.BigdataSail.BigdataSailConnection;
import com.bigdata.rdf.sail.BigdataSailRepository;
import com.bigdata.rdf.sail.BigdataSailRepositoryConnection;
import com.bigdata.rdf.store.BD;
import com.bigdata.rdf.store.BDS;
import com.bigdata.service.AbstractTransactionService;
import com.bigdata.journal.BufferMode;
import com.bigdata.journal.Journal;
import com.bigdata.rdf.axioms.NoAxioms;
import com.bigdata.rdf.sail.BigdataSail;
import com.bigdata.rdf.sail.BigdataSailRepository;
import com.bigdata.rdf.sail.RDRHistory;
import com.bigdata.rdf.store.AbstractTripleStore;
import com.blazegraph.gremlin.internal.Tinkerpop3CoreVocab_v10;
import com.blazegraph.gremlin.internal.Tinkerpop3ExtensionFactory;
import com.blazegraph.gremlin.internal.Tinkerpop3InlineURIFactory;
import com.blazegraph.gremlin.util.Code;

public class BlazegraphRepositoryProvider {
    public static BigdataSailRepository open(final String journalFile) {
        return open(journalFile, false);
    }

    public static BigdataSailRepository open(final String journalFile, final boolean init) {
        return open(getProperties(journalFile), init);
    }


    public static BigdataSailRepository open(final Properties props, final boolean init) {
        if (props.getProperty(Journal.Options.FILE) == null) {
            throw new IllegalArgumentException();
        }

        final BigdataSail sail = new BigdataSail(props);
        final BigdataSailRepository repo = new BigdataSailRepository(sail);
        if(init){
            Code.wrapThrow{  _ -> repo.initialize() };
        }
        return repo;
    }

    public static Properties getProperties(final String journalFile) {
        Properties p = new Properties()

        p.setProperty(Journal.Options.BUFFER_MODE, BufferMode.DiskRW.toString());

        /*
         * Turn off all RDFS/OWL inference.
         */
        p.setProperty(BigdataSail.Options.AXIOMS_CLASS, NoAxioms.class.getName());
        p.setProperty(BigdataSail.Options.TRUTH_MAINTENANCE, "false");
        p.setProperty(BigdataSail.Options.JUSTIFY, "false");

        /*
         * Turn on the text index.
         */
        //p.setProperty(BigdataSail.Options.TEXT_INDEX, "true");

        /*
         * Turn off quads and turn on statement identifiers.
         */
        p.setProperty(BigdataSail.Options.QUADS, "false");
        p.setProperty(BigdataSail.Options.STATEMENT_IDENTIFIERS, "true");

        /*
         * We will manage the grounding of sids manually.
         */
        p.setProperty(AbstractTripleStore.Options.COMPUTE_CLOSURE_FOR_SIDS, "false");

        /*
         * Inline string literals up to 10 characters (avoids dictionary indices
         * for short strings).
         */
        p.setProperty(AbstractTripleStore.Options.INLINE_TEXT_LITERALS, "true");
        p.setProperty(AbstractTripleStore.Options.MAX_INLINE_TEXT_LENGTH, "10");

        /*
         * Custom core Tinkerpop3 vocabulary.  Applications will probably want
         * to extend this.
         */
        p.setProperty(AbstractTripleStore.Options.VOCABULARY_CLASS, Tinkerpop3CoreVocab_v10.class.getName());

        /*
         * Use a multi-purpose inline URI factory that will auto-inline URIs
         * in the <blaze:> namespace.
         */
        p.setProperty(AbstractTripleStore.Options.INLINE_URI_FACTORY_CLASS, Tinkerpop3InlineURIFactory.class.getName());

        /*
         * Custom Tinkerpop3 extension factory for the ListIndexExtension IV,
         * used for Cardinality.list.
         */
        p.setProperty(AbstractTripleStore.Options.EXTENSION_FACTORY_CLASS, Tinkerpop3ExtensionFactory.class.getName());


        FileInputStream input = new FileInputStream("/runtime/confs/blazegraph.properties")
        Properties fromFile = new Properties()
        fromFile.load(input)

        // journal file
        p.setProperty(Journal.Options.FILE, journalFile);
        p.putAll(fromFile)

        //File f = new File("/runtime/confs/blazegraph.properties.2");
        //OutputStream out = new FileOutputStream( f );
        //p.store(out,  "");

        return p;
    }

}
