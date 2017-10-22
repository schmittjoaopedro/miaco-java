package schmitt.lab;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

public class ProblemUtils {

    public static void readProblem(String fileName) {
        fileName = (new File("scenarios")).getAbsolutePath() + "/" + fileName;
        try {
            File file = new File(fileName);
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            StringBuffer stringBuffer = new StringBuffer();
            String line;
            String delimiters = " :=\\n\\t\\r\\f\\v";
            while ((line = bufferedReader.readLine()) != null) {
                String keywords[] = getTokens(line, delimiters, true);
                if(keywords.length > 0) {
                    if("DIMENSION".equals(keywords[0])) {
                        ProgramData.problemSize = Integer.valueOf(keywords[1]);
                    }
                    else if("EDGE_WEIGHT_TYPE".equals(keywords[0]) && !"EUC_2D".equals(keywords[1])) {
                        System.out.println("Not EUC_2D");
                        System.exit(0);
                    }
                    else if("CAPACITY".equals(keywords[0])) {
                        ProgramData.maxCapacity = Integer.valueOf(keywords[1]);
                    }
                    else if("NODE_COORD_SECTION".equals(keywords[0])) {
                        if(ProgramData.problemSize != 0) {
                            ProgramData.problemSizeNoDepot = ProgramData.problemSize - 1;
                            ProgramData.maskedObjects = new Vertex[ProgramData.problemSize];
                            ProgramData.initObjects = new Vertex[ProgramData.problemSize];
                            for(int i = 0; i < ProgramData.problemSize; i++) {
                                keywords = getTokens(bufferedReader.readLine(), delimiters, true);
                                ProgramData.initObjects[i] = new Vertex();
                                ProgramData.initObjects[i].id = Integer.parseInt(keywords[0]) - 1;
                                ProgramData.initObjects[i].x = Double.parseDouble(keywords[1]);
                                ProgramData.initObjects[i].y = Double.parseDouble(keywords[2]);
                                ProgramData.maskedObjects[i] = new Vertex();
                                ProgramData.maskedObjects[i].id = ProgramData.initObjects[i].id;
                                ProgramData.maskedObjects[i].x = ProgramData.initObjects[i].x;
                                ProgramData.maskedObjects[i].y = ProgramData.initObjects[i].y;
                            }
                            ProgramData.distances = new double[ProgramData.problemSize][ProgramData.problemSize];
                            ProblemUtils.computeDistances();
                        }
                    }
                    else if("DEMAND_SECTION".equals(keywords[0])) {
                        if (ProgramData.problemSize != 0) {
                            ProgramData.maskedDemand = new int[ProgramData.problemSize];
                            ProgramData.initDemand = new int[ProgramData.problemSize];
                            for (int i = 0; i < ProgramData.problemSize; i++) {
                                keywords = getTokens(bufferedReader.readLine(), delimiters, true);
                                ProgramData.initDemand[i] = Integer.parseInt(keywords[1]);
                                ProgramData.maskedDemand[i] = ProgramData.initDemand[i];
                            }
                        }
                    }
                    else if("DEPOT_SECTION".equals(keywords[0])) {
                        keywords = getTokens(bufferedReader.readLine(), delimiters, true);
                        ProgramData.depot = Integer.parseInt(keywords[0]) - 1;
                    }
                }
            }
            fileReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String[] getTokens(String str, String delim, boolean trim){
        StringTokenizer stok = new StringTokenizer(str, delim);
        String tokens[] = new String[stok.countTokens()];
        for(int i=0; i<tokens.length; i++){
            tokens[i] = stok.nextToken();
            if(trim)
                tokens[i] = tokens[i].trim();
        }
        return tokens;
    }

    public static void computeDistances() {
        for (int i = 0; i < ProgramData.problemSize; i++) {
            for (int j = 0; j < ProgramData.problemSize; j++) {
                ProgramData.distances[i][j] = euclideanDistance(i, j);
            }
        }
    }

    public static double euclideanDistance(int i, int j) {
        double xd = ProgramData.maskedObjects[i].x - ProgramData.maskedObjects[j].x;
        double yd = ProgramData.maskedObjects[i].y - ProgramData.maskedObjects[j].y;
        return (int) (Math.sqrt(xd * xd + yd * yd) + 0.5);
    }

}
