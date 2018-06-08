import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 *
 * @author Jonathan Ellithorpe <jde@cs.stanford.edu>
 */
public class LDBCGraphLoader {

  private static  boolean SKIP_COMMIT = false;
  private static final long TX_MAX_RETRIES = 1000;

  public static void loadVertices(Graph graph, Path filePath,  boolean printLoadingDots, int batchSize, long progReportPeriod) {

    String[] colNames = null;
    boolean firstLine = true;
    Map<Object, Object> propertiesMap;
    SimpleDateFormat birthdayDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    birthdayDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    SimpleDateFormat creationDateDateFormat =
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    creationDateDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    def fileNameParts = filePath.getFileName().toString().split("_");
    String entityName = fileNameParts[0];

    List<String> lines = Files.readAllLines(filePath);
    colNames = lines.get(0).split("\\|");
    long lineCount = 0;
    boolean txSucceeded;
    long txFailCount;

    // For progress reporting
    long startTime = System.currentTimeMillis();
    long nextProgReportTime = startTime + progReportPeriod*1000;
    long lastLineCount = 0;

    for (int startIndex = 1; startIndex < lines.size();
        startIndex += batchSize) {
      int endIndex = Math.min(startIndex + batchSize, lines.size());

        for (int i = startIndex; i < endIndex; i++) {
          String line = lines.get(i);

          String[] colVals = line.split("\\|");
          propertiesMap = new HashMap<>();

          for (int j = 0; j < colVals.length; ++j) {
            if (colNames[j].equals("id")) {
              propertiesMap.put("iid", entityName + ":" + colVals[j]);
            } else if (colNames[j].equals("birthday")) {
              propertiesMap.put(colNames[j], String.valueOf(
                    birthdayDateFormat.parse(colVals[j]).getTime()));
            } else if (colNames[j].equals("creationDate")) {
              propertiesMap.put(colNames[j], String.valueOf(
                    creationDateDateFormat.parse(colVals[j]).getTime()));
            } else {
              propertiesMap.put(colNames[j], colVals[j]);
            }
          }

          propertiesMap.put(T.label, entityName);

          List<Object> keyValues = new ArrayList<>();
          propertiesMap.forEach{ key, val ->
            keyValues.add(key);
            keyValues.add(val);
          };

          graph.addVertex(keyValues.toArray());

          lineCount++;
        }

        if(!SKIP_COMMIT){
        try {
          graph.tx().commit();
        } catch (UnsupportedOperationException e) {
         System.err.println("Does not support g.tx().commit(). Ignoring.");
         SKIP_COMMIT = true
        } catch (Exception e) {
          System.err.println( "ERROR: Transaction failed times, aborting... at " + startIndex + " " + (endIndex-1) + " for  " + e.getClass() + " "  + e.getMessage());
        }
        }


      if (printLoadingDots &&
          (System.currentTimeMillis() > nextProgReportTime)) {
        long timeElapsed = System.currentTimeMillis() - startTime;
        long linesLoaded = lineCount - lastLineCount;
        System.out.println(String.format(
              "Time Elapsed: %s, Lines Loaded: %d",
              (timeElapsed/1000)/60, linesLoaded));
        nextProgReportTime += progReportPeriod*1000;
        lastLineCount = lineCount;
      }
    }
      long timeElapsed = System.currentTimeMillis() - startTime;
        long linesLoaded = lineCount ;
        System.out.println(String.format(
              "Time Elapsed: %s, Lines Loaded: %d",
              (timeElapsed/1000)/60, linesLoaded));
        nextProgReportTime += progReportPeriod*1000;
        lastLineCount = lineCount;
  }

  public static void loadProperties(Graph graph, Path filePath, boolean printLoadingDots, int batchSize, long progReportPeriod)  {
    long count = 0;
    String[] colNames = null;
    boolean firstLine = true;
    def fileNameParts = filePath.getFileName().toString().split("_");
    String entityName = fileNameParts[0];

    List<String> lines = Files.readAllLines(filePath);
    colNames = lines.get(0).split("\\|");
    long lineCount = 0;
    boolean txSucceeded;
    long txFailCount;

    // For progress reporting
    long startTime = System.currentTimeMillis();
    long nextProgReportTime = startTime + progReportPeriod*1000;
    long lastLineCount = 0;

    for (int startIndex = 1; startIndex < lines.size();
        startIndex += batchSize) {
      int endIndex = Math.min(startIndex + batchSize, lines.size());
      txSucceeded = false;
      txFailCount = 0;

        for (int i = startIndex; i < endIndex; i++) {
          String line = lines.get(i);

          String[] colVals = line.split("\\|");

          GraphTraversalSource g = graph.traversal();
          Vertex vertex =
            g.V().has("iid", entityName + ":" + colVals[0]).next();

          for (int j = 1; j < colVals.length; ++j) {
            vertex.property(VertexProperty.Cardinality.list, colNames[j],
                colVals[j]);
          }

          lineCount++;
        }

        if(!SKIP_COMMIT){
        try {
          graph.tx().commit();
        } catch (UnsupportedOperationException e) {
         System.err.println("Does not support g.tx().commit(). Ignoring.");
         SKIP_COMMIT = true
        } catch (Exception e) {
          System.err.println( "ERROR: Transaction failed times, aborting... at " + startIndex + " " + (endIndex-1) + " for  " + e.getClass() + " "  + e.getMessage());
        }
        }


      if (printLoadingDots &&
          (System.currentTimeMillis() > nextProgReportTime)) {
        long timeElapsed = System.currentTimeMillis() - startTime;
        long linesLoaded = lineCount - lastLineCount;
        System.out.println(String.format(
              "Time Elapsed: %s, Lines Loaded: %d",
              (timeElapsed/1000)/60, linesLoaded));
        nextProgReportTime += progReportPeriod*1000;
        lastLineCount = lineCount;
      }
    }

    long timeElapsed = System.currentTimeMillis() - startTime;
        long linesLoaded = lineCount ;
        System.out.println(String.format(
              "Time Elapsed: %s, Lines Loaded: %d",
              (timeElapsed/1000)/60, linesLoaded));
        nextProgReportTime += progReportPeriod*1000;
        lastLineCount = lineCount;
  }

  public static void loadEdges(Graph graph, Path filePath, boolean undirected,
      boolean printLoadingDots, int batchSize, long progReportPeriod)
      throws IOException,  java.text.ParseException {
    long count = 0;
    String[] colNames = null;
    boolean firstLine = true;
    Map<Object, Object> propertiesMap;
    SimpleDateFormat creationDateDateFormat =
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    creationDateDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    SimpleDateFormat joinDateDateFormat =
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    joinDateDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    def fileNameParts = filePath.getFileName().toString().split("_");
    String v1EntityName = fileNameParts[0];
    String edgeLabel = fileNameParts[1];
    String v2EntityName = fileNameParts[2];

    List<String> lines = Files.readAllLines(filePath);
    colNames = lines.get(0).split("\\|");
    long lineCount = 0;
    boolean txSucceeded;
    long txFailCount;

    // For progress reporting
    long startTime = System.currentTimeMillis();
    long nextProgReportTime = startTime + progReportPeriod*1000;
    long lastLineCount = 0;

    for (int startIndex = 1; startIndex < lines.size();
        startIndex += batchSize) {
      int endIndex = Math.min(startIndex + batchSize, lines.size());
      txSucceeded = false;
      txFailCount = 0;

        for (int i = startIndex; i < endIndex; i++) {
          String line = lines.get(i);

          String[] colVals = line.split("\\|");

          GraphTraversalSource g = graph.traversal();
          def Vertex vertex1;
          def Vertex vertex2;
          try {
          vertex1 =
            g.V().has("iid", v1EntityName + ":" + colVals[0]).next();
          vertex2 =
            g.V().has("iid", v2EntityName + ":" + colVals[1]).next();
          } catch(FastNoSuchElementException e ){
            System.err.println("Missing One of "  + v1EntityName + ":" + colVals[0] + "    -    "  +  v2EntityName + ":" + colVals[1]);
          }
          propertiesMap = new HashMap<>();
          for (int j = 2; j < colVals.length; ++j) {
            if (colNames[j].equals("creationDate")) {
              propertiesMap.put(colNames[j], String.valueOf(
                    creationDateDateFormat.parse(colVals[j]).getTime()));
            } else if (colNames[j].equals("joinDate")) {
              propertiesMap.put(colNames[j], String.valueOf(
                    joinDateDateFormat.parse(colVals[j]).getTime()));
            } else {
              propertiesMap.put(colNames[j], colVals[j]);
            }
          }

          List<Object> keyValues = new ArrayList<>();
          propertiesMap.forEach{ key, val ->
            keyValues.add(key);
            keyValues.add(val);
          };

          vertex1.addEdge(edgeLabel, vertex2, keyValues.toArray());

          if (undirected) {
            vertex2.addEdge(edgeLabel, vertex1, keyValues.toArray());
          }

          lineCount++;
        }

        if(!SKIP_COMMIT){
        try {
          graph.tx().commit();
        } catch (UnsupportedOperationException e) {
         System.err.println("Does not support g.tx().commit(). Ignoring.");
         SKIP_COMMIT = true
        } catch (Exception e) {
          System.err.println( "ERROR: Transaction failed times, aborting... at " + startIndex + " " + (endIndex-1) + " for  " + e.getClass() + " "  + e.getMessage());
        }
        }


      if (printLoadingDots &&
          (System.currentTimeMillis() > nextProgReportTime)) {
        long timeElapsed = System.currentTimeMillis() - startTime;
        long linesLoaded = lineCount - lastLineCount;
        System.out.println(String.format(
              "Time Elapsed: %s, Lines Loaded: %d",
              (timeElapsed/1000)/60, linesLoaded));
        nextProgReportTime += progReportPeriod*1000;
        lastLineCount = lineCount;
      }
    }
    long timeElapsed = System.currentTimeMillis() - startTime;
        long linesLoaded = lineCount ;
        System.out.println(String.format(
              "Time Elapsed: %s, Lines Loaded: %d",
              (timeElapsed/1000)/60, linesLoaded));
        nextProgReportTime += progReportPeriod*1000;
        lastLineCount = lineCount;
  }
}

inputBaseDir = "/tmp/ldbc-out/social_network/"
graph = TinkerGraph.open();
System.out.println( "Num nodes " + graph.traversal().V().count().next());
System.out.println( "Num edges " + graph.traversal().E().count().next());

batchSize = 1000
progReportPeriod = 50

  vertexLabels = [  "person",      "comment",      "forum",      "organisation",      "place",      "post",      "tag",      "tagclass" ];

    edgeLabels = [
      "containerOf",
      "hasCreator",
      "hasInterest",
      "hasMember",
      "hasModerator",
      "hasTag",
      "hasType",
      "isLocatedIn",
      "isPartOf",
      "isSubclassOf",
      "knows",
      "likes",
      "replyOf",
      "studyAt",
      "workAt"
    ];

    // All property keys with Cardinality.SINGLE
    singleCardPropKeys = [
      "birthday", // person
      "browserUsed", // comment person post
      "classYear", // studyAt
      "content", // comment post
      "creationDate", // comment forum person post knows likes
      "firstName", // person
      "gender", // person
      "imageFile", // post
      "joinDate", // hasMember
      //"language", // post
      "lastName", // person
      "length", // comment post
      "locationIP", // comment person post
      "name", // organisation place tag tagclass
      "title", // forum
      "type", // organisation place
      "url", // organisation place tag tagclass
      "workFrom", // workAt
    ];

    // All property keys with Cardinality.LIST
    listCardPropKeys = [
      "email", // person
      "language" // person, post
    ];



    // TODO: Make file list generation programmatic. This method of loading,
    // however, will be far too slow for anything other than the very
    // smallest of SNB graphs, and is therefore quite transient. This will
    // do for now.
    nodeFiles = [
      "person_0_0.csv",
      "comment_0_0.csv",
      "forum_0_0.csv",
      "organisation_0_0.csv",
      "place_0_0.csv",
      "post_0_0.csv",
      "tag_0_0.csv",
      "tagclass_0_0.csv"
    ];

    propertiesFiles = [
      "person_email_emailaddress_0_0.csv",
      "person_speaks_language_0_0.csv"
    ];

    edgeFiles = [
      "comment_hasCreator_person_0_0.csv",
      "comment_hasTag_tag_0_0.csv",
      "comment_isLocatedIn_place_0_0.csv",
      "comment_replyOf_comment_0_0.csv",
      "comment_replyOf_post_0_0.csv",
      "forum_containerOf_post_0_0.csv",
      "forum_hasMember_person_0_0.csv",
      "forum_hasModerator_person_0_0.csv",
      "forum_hasTag_tag_0_0.csv",
      "organisation_isLocatedIn_place_0_0.csv",
      "person_hasInterest_tag_0_0.csv",
      "person_isLocatedIn_place_0_0.csv",
      "person_knows_person_0_0.csv",
      "person_likes_comment_0_0.csv",
      "person_likes_post_0_0.csv",
      "person_studyAt_organisation_0_0.csv",
      "person_workAt_organisation_0_0.csv",
      "place_isPartOf_place_0_0.csv",
      "post_hasCreator_person_0_0.csv",
      "post_hasTag_tag_0_0.csv",
      "post_isLocatedIn_place_0_0.csv",
      "tag_hasType_tagclass_0_0.csv",
      "tagclass_isSubclassOf_tagclass_0_0.csv"
    ];

    try {
      for (String fileName : nodeFiles) {
        System.out.print("Loading node file " + fileName + " ");
        try {
          LDBCGraphLoader.loadVertices(graph, Paths.get(inputBaseDir + "/" + fileName),
              true, batchSize, progReportPeriod);
          System.out.println("Finished");
        } catch (NoSuchFileException e) {
          System.out.println(" File not found.");
        }
      }

      for (String fileName : propertiesFiles) {
        System.out.print("Loading properties file " + fileName + " ");
        try {
          LDBCGraphLoader.loadProperties(graph, Paths.get(inputBaseDir + "/" + fileName),
              true, batchSize, progReportPeriod);
          System.out.println("Finished");
        } catch (NoSuchFileException e) {
          System.out.println(" File not found.");
        }
      }

      for (String fileName : edgeFiles) {
        System.out.print("Loading edge file " + fileName + " ");
        try {
          if (fileName.contains("person_knows_person")) {
            LDBCGraphLoader.loadEdges(graph, Paths.get(inputBaseDir + "/" + fileName), true,
                true, batchSize, progReportPeriod);
          } else {
            LDBCGraphLoader.loadEdges(graph, Paths.get(inputBaseDir + "/" + fileName), false,
                true, batchSize, progReportPeriod);
          }

          System.out.println("Finished");
        } catch (NoSuchFileException e) {
          System.out.println(" File not found.");
        }
      }

      System.out.println("Done Loading!");
      System.out.println( "Num nodes " + graph.traversal().V().count().next());
      System.out.println( "Num edges " + graph.traversal().E().count().next());
      graph.io(IoCore.gryo()).writeGraph("/runtime/data/social_network.1000u_1y.kryo");

      try  {
          os = new FileOutputStream("/runtime/data/social_network.1000u.1y.json")
          mapper = mapper = graph.io(graphson()).mapper().embedTypes(true).create()
          graph.io(IoCore.graphson()).writer().mapper(mapper).create().writeGraph(os, graph)
      } catch (Exception e) {
      System.out.println("Exception: " + e);
      e.printStackTrace();
    }

    } catch (Exception e) {
      System.out.println("Exception: " + e);
      e.printStackTrace();
    } finally {
      graph.close();
    }


  System.exit(0);
