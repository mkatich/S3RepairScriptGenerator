package s3repairscriptgenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TextFileIO {
    
    //MEMBER VARIABLES
    
    //MEMBER VARIABLES FOR BASIC TextFileIO FILES
    private File file;
    private String fileAbsPath;
    private String cellDelimiter;
    private boolean hasFileHeader;
    private boolean isOpenForWriting;
    private boolean isOpenForReading;
    private long numLinesWritten;
    private FileWriter fw;
    private BufferedWriter bw;
    private FileReader fr;
    private BufferedReader br;
    
    //MEMBER VARIABLES FOR TextFileIO FILES WITH TABULAR DATA
    private String[] columnHeader;
    private boolean isValidOutputScriptFile;
    private String tableTitleInFileHeaderSeparatorLine;
    private int tableHeaderLineNum;//Line number of data table header (file starts with line 1). If not known or not written yet, then it's -1
    private long numTableRows;
    
    //Delimiters - Some info about the possible table cell delimiters
    //- comma is default separator
    //- tab separator can be specified by "\t". It's easily used with Excel and tolerates commas in values with no need for quotes around values
    
    
    
    //CONSTRUCTORS
    //If delimiter not specified, comma is used.
    //If hasFileHeader not specified, it is assumed to be false.
    //If tableTitleInFileHeaderSeparatorLine not specified, blank String is used.
    
    //Constructors below create a new TextFileIO when the file doesn't already exist.
    //Constructors can also be  used to create a new TextFileIO objecbt when the 
    //underlying file already exists, but they don't check if it exists. Use exists()
    //method for that.
    
    //CONSTRUCTOR (takes file name and destination path as parameter)
    public TextFileIO(String fileAbsPath) {
        this(new File(fileAbsPath), new String[0], ",", false, "");
    }
    //CONSTRUCTOR (takes file name and destination path, and String array holding column header names as parameters)
    public TextFileIO(File f, String[] colHeader, String cellDelimiter, boolean hasFileHeader, String tableTitleInFileHeaderSeparatorLine) {
        //set member vars
        this.file = f;
        this.fileAbsPath = f.getAbsolutePath();
        this.cellDelimiter = cellDelimiter;//comma separator since none specified
        this.hasFileHeader = hasFileHeader;
        this.isOpenForWriting = false;
        this.isOpenForReading = false;
        this.numLinesWritten = 0;
        this.columnHeader = colHeader;
        this.isValidOutputScriptFile = false;
        this.tableTitleInFileHeaderSeparatorLine = tableTitleInFileHeaderSeparatorLine;
        this.tableHeaderLineNum = -1;
        this.numTableRows = 0;
    }
    
    //GETTERS
    public File getFile() {
        return file;
    }

    public String getAbsPath() {
        return this.fileAbsPath;
    }
    
    public String getName() {
        return this.file.getName();
    }
    
    public boolean exists() {
        return this.file.exists();
    }

    public boolean hasFileHeader() {
        return hasFileHeader;
    }
    
    public boolean isOpenForWriting() {
        return this.isOpenForWriting;
    }

    public boolean isOpenForReading() {
        return this.isOpenForReading;
    }

    public long getNumLinesWritten() {
        return numLinesWritten;
    }

    public boolean isIsValidOutputScriptFile() {
        return isValidOutputScriptFile;
    }
    
    public int getTableHeaderLineNum() {
        return tableHeaderLineNum;
    }
    
    public long getNumTableRows() {
        return this.numTableRows;
    }
    
    
    
    //Method canWrite() checks if the file exists and this application is allowed to write to it.
    public boolean canWrite() {
        return this.file.canWrite();
    }
    //Method canRead checks if hte file exists and this application is allowed to read from it.
    private boolean canRead() {
        return this.file.canRead();
    }
    
    
    
    //METHODS FOR MANAGING THE WRITING AND READING RESOURCES BELOW
    
    //Method openForWriting() opens writing resources
    public boolean openForWriting() {
        boolean openSuccess = false;
        try {
            boolean append = false;//Append mode. If false, it overwrites
            if (!append) {
                //Being absolutely certain we're starting from scratch
                Files.deleteIfExists(Paths.get(fileAbsPath));
                Files.createFile(Paths.get(fileAbsPath));
            }
            fw = new FileWriter(this.file, append);
            bw = new BufferedWriter(fw);
            if (this.isOpenForWriting) {
                System.out.println("TextFileIO "+this.fileAbsPath+" was already open for writing, but we re-opened (ready to append to end of file)");
            }
            this.isOpenForWriting = true;
            openSuccess = true;
        }
        catch (IOException e) {
            StringWriter swStackTrace = new StringWriter();
            e.printStackTrace(new PrintWriter(new StringWriter()));
            System.out.println("ERROR in TextFileIO.openForWriting() - \n"+swStackTrace.toString());
        }
        return openSuccess;
    }
    
    //Method closeWriting() closes writing resources, and flushes first to make sure all changes took effect.
    public void closeWriting() {
        try {
            //flush buffer so everything writes and close it up
            bw.flush();
            bw.close();
            fw.close();
        }
        catch (IOException e) {
            StringWriter swStackTrace = new StringWriter();
            e.printStackTrace(new PrintWriter(new StringWriter()));
            System.out.println("ERROR in TextFileIO.closeWriting() - \n"+swStackTrace.toString());
        }
    }
    
    //Method openForReading() opens reading resources
    public boolean openForReading() {
        boolean openSuccess = false;
        if (canRead()){
            try {
                this.fr = new FileReader(this.file);//false for appending to end of file
                this.br = new BufferedReader(fr);
                if (this.isOpenForReading)
                    System.out.println("TextFileIO "+this.fileAbsPath+" was already open for reading, but we re-opened");
                this.isOpenForReading = true;
                openSuccess = true;
            }
            catch (IOException e) {
                StringWriter swStackTrace = new StringWriter();
                e.printStackTrace(new PrintWriter(new StringWriter()));
                System.out.println("ERROR in TextFileIO.openForReading() - \n"+swStackTrace.toString());
            }
        }
        else {
            System.out.println("ERROR in TextFileIO.openForReading() - we are unable to read to the file "+this.fileAbsPath);
        }
        return openSuccess;
    }
    
    //Method closeReading() closes reading resources.
    public void closeReading() {
        try {
            br.close();
            fr.close();
            this.isOpenForReading = false;
        }
        catch (IOException e) {
            StringWriter swStackTrace = new StringWriter();
            e.printStackTrace(new PrintWriter(new StringWriter()));
            System.out.println("ERROR in TextFileIO.closeReading() - \n"+swStackTrace.toString());
        }
    }
    
    
    
    //METHODS FOR READING LINES
    //Method readLine() reads a line at the current spot in the file to a String and returns that
    //This was added specifically for the SpecializedRaidDestriper app which reads
    //a SequenceFile (TextFileIO) to enable destriping very strange RAIDs.
    public String readLine() {
        String lineContents = null;
        try {
            if (this.isOpenForReading)
                lineContents = br.readLine();
        }
        catch (IOException e) {
            StringWriter swStackTrace = new StringWriter();
            e.printStackTrace(new PrintWriter(new StringWriter()));
            System.out.println("ERROR in TextFileIO.writeLine() - \n"+swStackTrace.toString());
        }
        return lineContents;
    }
    
    
    
    //METHODS FOR WRITING LINES
    //Method writeLine() writes a generic line using a given String, and a newline.
    public boolean writeLine(String newLogLine) {
        boolean writeSuccess = false;
        if (this.isOpenForWriting){
            try {
                bw.write(newLogLine);
                //bw.newLine();//newline - This writes OS specific newline, which on Windows is \r\n, and that's bad for Mac
                bw.write("\n");//newline, works on both Windows and Mac
                bw.flush();//flush to force write so progress is viewable on the fly or if the process ends unnaturally
                writeSuccess = true;
                numLinesWritten++;
            }
            catch (IOException e) {
                StringWriter swStackTrace = new StringWriter();
                e.printStackTrace(new PrintWriter(new StringWriter()));
                System.out.println("ERROR in TextFileIO.writeLine() - \n"+swStackTrace.toString());
            }
        }
        else 
            System.out.println("ERROR in TextFileIO.writeLine() - file is not open for writing");
        return writeSuccess;
    }
    //Method writeTableDataLine() writes a line of tabular data using a given String CSV
    //public boolean writeTableDataLine(String newLogLineCsv) {
    //    return writeTableDataLine((new CSVSplitter()).split(newLogLineCsv));
    //}
    //Method writeTableDataLine() writes a line of tabular data using a given String array.
    public boolean writeTableDataLine(String[] newLogLine) {
        boolean writeSuccess = false;
        if (this.isOpenForWriting){
            try {
                for (int i = 0; i < newLogLine.length; i++){
                    if (i > 0)
                        bw.write(this.cellDelimiter);
                    bw.write(newLogLine[i]);
                }
                //bw.newLine();//newline - This writes OS specific newline, which on Windows is \r\n, and that's bad for Mac
                bw.write("\n");//newline, works on both Windows and Mac
                bw.flush();//flush to force write so progress is viewable on the fly or if the process ends unnaturally
                writeSuccess = true;
                numLinesWritten++;
            }
            catch (IOException e) {
                StringWriter swStackTrace = new StringWriter();
                e.printStackTrace(new PrintWriter(new StringWriter()));
                System.out.println("ERROR in TextFileIO.writeTableDataLine() - \n"+swStackTrace.toString());
            }
        }
        else 
            System.out.println("ERROR in TextFileIO.writeTableDataLine() - file is not open for writing");
        return writeSuccess;
    }
    //Method writeFileHeaderSeparatorLine() writes the special file header separator line. It will also contain 
    //a table title if one has been set.
    public boolean writeFileHeaderSeparatorLine() {
        boolean writeSuccess;
        String newLogLine = "*****";
        if (!this.tableTitleInFileHeaderSeparatorLine.equals(""))
            newLogLine += " "+this.tableTitleInFileHeaderSeparatorLine+" ";
        newLogLine += "*****";
        writeSuccess = writeLine(newLogLine);
        return writeSuccess;
    }
    //Method writeEmptyLine() writes an empty line to the file.
    public boolean writeEmptyLine() {
        boolean writeSuccess = false;
        if (this.isOpenForWriting){
            try {
                //write newline and close
                //bw.newLine();//newline - This writes OS specific newline, which on Windows is \r\n, and that's bad for Mac
                bw.write("\n");//newline, works on both Windows and Mac
                bw.flush();//force it to write so we can be sure it's viewable on the fly or if the process ends unnaturally
                writeSuccess = true;
            }
            catch (IOException e) {
                StringWriter swStackTrace = new StringWriter();
                e.printStackTrace(new PrintWriter(new StringWriter()));
                System.out.println("ERROR in TextFileIO.writeEmptyLine() - \n"+swStackTrace.toString());
            }
        }
        else 
            System.out.println("ERROR in TextFileIO.writeEmptyLine() - file is not open for writing");
        return writeSuccess;
    }
    //Method writeTableHeadersLine() writes the table header line.
    public boolean writeTableHeadersLine() {
        return writeTableDataLine(this.columnHeader);
    }
    
}

