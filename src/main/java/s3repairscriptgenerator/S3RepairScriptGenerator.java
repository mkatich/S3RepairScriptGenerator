
package s3repairscriptgenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Michael
 */
public class S3RepairScriptGenerator {
    
    //User parameters
    //
    // * Path to affected files list
    // * Which script(s) to build (delete and/or upload)
    // * S3 bucket path
    // * S3 endpoint URL
    // * Path to good backup files to be uploaded
    // * Test mode (hidden option)
    // * Path of directory where to write script files
    
    static final String APPNAME = "BennyHinnReplaceCorruptFiles";
    
    //Path to the file that contains the list of affected/bad objects
    static String affectedFilesListPath = null;
    
    //Indicate which script to build
    static String scriptToBuild = "delete and upload";// "delete", "upload", or "delete and upload"
    
    static String s3BucketPath = null;//"s3://mybucketname/"
    
    static String s3EndpointUrl = null;//"https://s3.us-central-1.wasabisys.com"
    
    //Path to the parent directory of all the good backup files we'll use for uploading (if chosen)
    static String goodBackupFilesPath = null;
    
    //Indicate whether we're doing this for testing or production - chooses input files
    static boolean testMode = false;
    
    //Path to directory in which to write output script files
    static String outputScriptFilesDir = null;
    
    
    //Some methods for processing user parameters from CLI
    private static String getArg(String[] args, String flag) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(flag)) {
                return args[i + 1];
            }
        }
        return null;
    }
    private static boolean hasFlag(String[] args, String flag) {
        for (String arg : args) {
            if (arg.equals(flag)) return true;
        }
        return false;
    }
    private static void printUsage() {
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  java -jar generator.jar \\");
        System.out.println("      --affected <path-to-affected-files> \\");
        System.out.println("      --script delete|upload|both \\");
        System.out.println("      --bucket <s3-bucket-name> \\");
        System.out.println("      --endpoint <s3-endpoint-url> \\");
        System.out.println("      [--backup <path-to-good-backups>] \\");
        System.out.println("      [--test-mode]");
        System.out.println("      --outdir <output-directory> \\");
        System.out.println();
        System.out.println("Notes:");
        System.out.println("    --backup is REQUIRED when --script is 'upload' or 'both'.");
        System.out.println();
    }
    
    
    /*
    //Example usage below
    //(Either put these commands all on one line, or use ^ for Windows CMD or \ for Bash)
    
    //Build both delete and upload scripts:
    java -jar generator.jar 
        --affected "D:/fix_files/AffectedFilesList.txt" 
        --script both 
        --bucket mybucketname 
        --endpoint https://s3.us-central-1.storageprovider.com 
        --backup "Z:/" 
        --outdir "D:/fix_files/"
    
    //Build only the delete script:
    java -jar generator.jar 
        --affected "D:/fix_files/AffectedFilesList.txt" 
        --script delete 
        --bucket mybucketname 
        --endpoint https://s3.us-central-1.storageprovider.com 
        --outdir "D:/fix_files/"
    
    //Build only the upload script, and in test mode:
    java -jar generator.jar 
        --affected "D:/fix_files/AffectedFilesList.txt" 
        --script upload 
        --bucket mybucketname 
        --endpoint https://s3.us-central-1.storageprovider.com 
        --backup "Z:/" 
        --outdir "D:/fix_files/"
    
    */
    
    public static void main(String[] args) {
        
        //CLI parameter parsing
        String argScript  = getArg(args, "--script");// delete | upload | both
        String argAffectedList = getArg(args, "--affected");//REQUIRED
        String argBackupDir   = getArg(args, "--backup");//REQUIRED only for upload/both
        String argOutputDir   = getArg(args, "--outdir");//REQUIRED
        boolean argTestMode = hasFlag(args, "--test-mode");//optional flag
        String argBucket = getArg(args, "--bucket");//REQUIRED
        String argEndpoint = getArg(args, "--endpoint");//REQUIRED
        
        //Set parameter values, if value was provided
        if (argScript != null) 
            scriptToBuild = argScript;
        if (argAffectedList != null) 
            affectedFilesListPath = argAffectedList;
        if (argBackupDir != null) 
            goodBackupFilesPath = argBackupDir;
        if (argOutputDir != null) 
            outputScriptFilesDir = argOutputDir;
        if (argTestMode) 
            testMode = true;
        if (argBucket != null) 
            s3BucketPath = argBucket;
        if (argEndpoint != null) 
            s3EndpointUrl = argEndpoint;
        
        //Validate scriptToBuild parameter first
        if (scriptToBuild == null 
                || (!scriptToBuild.equals("delete") 
                    && !scriptToBuild.equals("upload") 
                    && !scriptToBuild.equals("both"))) {
            System.out.println("ERROR: Invalid --script value. Use: delete | upload | both");
            printUsage();
            return;
        }
        
        //Adjust/normalize parameter
        if (scriptToBuild.equals("both")) {
            scriptToBuild = "delete and upload";
        }
        
        //Validate
        if (affectedFilesListPath == null) {
            System.out.println("ERROR: Missing --affected <path>");
            printUsage();
            return;
        }
        if (outputScriptFilesDir == null) {
            System.out.println("ERROR: Missing --outdir <path>");
            printUsage();
            return;
        }
        if (goodBackupFilesPath == null && scriptToBuild.contains("upload")) {
            System.out.println("ERROR: Missing --backup <path>, required for building upload script");
            printUsage();
            return;
        }
        if (s3BucketPath == null) {
            System.out.println("ERROR: Missing required parameter: --bucket <bucket-name>");
            printUsage();
            return;
        }
        if (s3EndpointUrl == null) {
            System.out.println("ERROR: Missing required parameter: --endpoint <endpoint-url>");
            printUsage();
            return;
        }
        
        //Print parameters summary for user 
        System.out.println("");
        System.out.println("=== "+APPNAME+" ===");
        System.out.println(""
                + "Build a script for either one or both of "
                + "- deleting bad files in an S3 bucket as identified in the affected files "
                + "list, uploading good backup files to the S3 bucket to replace "
                + "the affected bad files. It builds the script file(s) and does not execute them.");
        System.out.println("Affected files list: " + affectedFilesListPath);
        System.out.println("Scripts to build: " + scriptToBuild);
        System.out.println("S3 bucket: " + s3BucketPath);
        System.out.println("S3 endpoint: " + s3EndpointUrl);
        System.out.println("Backup files dir: " + goodBackupFilesPath);
        if (testMode) {
            System.out.println("Test mode: " + testMode);
        }
        System.out.println("Output dir: " + outputScriptFilesDir);
        System.out.println("====================================");
        
        //Get an output file object for the text file containing the list of affected files
        TextFileIO affectedFilesList = new TextFileIO(affectedFilesListPath);
        if (!affectedFilesList.exists()){
            //okToExecute = false;
            System.out.println("ERROR: File "+affectedFilesList.getAbsPath()+" does not exist");
        }
        
        //Open the input file
        boolean fileOpenedSuccess = affectedFilesList.openForReading();
        if (!fileOpenedSuccess){
            System.out.println("ERROR: File "+affectedFilesList.getAbsPath()+" could not be read");
        }
        
        //Set up the output file(s) - create and open for writing
        String outputScriptFilePath_delete_powershell;
        String outputScriptFilePath_delete_bash;
        String outputScriptFilePath_upload_powershell;
        String outputScriptFilePath_upload_bash;
        TextFileIO outputScriptFile_delete_powershell = null;
        TextFileIO outputScriptFile_delete_bash = null;
        TextFileIO outputScriptFile_upload_powershell = null;
        TextFileIO outputScriptFile_upload_bash = null;
        if (scriptToBuild.contains("delete")) {
            outputScriptFilePath_delete_powershell = outputScriptFilesDir+"DeleteBadFiles_PowerShell.ps1";
            outputScriptFilePath_delete_bash =       outputScriptFilesDir+"DeleteBadFiles_Bash.sh";
            if (testMode) {
                outputScriptFilePath_delete_powershell = outputScriptFilesDir+"DeleteBadFiles_PowerShell_TEST.ps1";
                outputScriptFilePath_delete_bash =       outputScriptFilesDir+"DeleteBadFiles_Bash_TEST.sh";
            }
            outputScriptFile_delete_powershell = new TextFileIO(outputScriptFilePath_delete_powershell);
            outputScriptFile_delete_powershell.openForWriting();
            outputScriptFile_delete_bash = new TextFileIO(outputScriptFilePath_delete_bash);
            outputScriptFile_delete_bash.openForWriting();
        }
        if (scriptToBuild.contains("upload")){
            outputScriptFilePath_upload_powershell = outputScriptFilesDir+"UploadGoodFiles_PowerShell.ps1";
            outputScriptFilePath_upload_bash =       outputScriptFilesDir+"UploadGoodFiles_Bash.sh";
            if (testMode) {
                outputScriptFilePath_upload_powershell = outputScriptFilesDir+"UploadGoodFiles_PowerShell_TEST.ps1";
                outputScriptFilePath_upload_bash =       outputScriptFilesDir+"UploadGoodFiles_Bash_TEST.sh";
            }
            outputScriptFile_upload_powershell = new TextFileIO(outputScriptFilePath_upload_powershell);
            outputScriptFile_upload_powershell.openForWriting();
            outputScriptFile_upload_bash = new TextFileIO(outputScriptFilePath_upload_bash);
            outputScriptFile_upload_bash.openForWriting();
        }
        
        
        //For Bash, need to add something to beginning of the ouptput script(s)
        if (scriptToBuild.contains("delete")){
            outputScriptFile_delete_bash.writeLine("#!/bin/bash");
            outputScriptFile_delete_bash.writeLine("");
            outputScriptFile_delete_bash.writeLine("ERROR_LOG=\""+APPNAME+" - errors.log\"");
            outputScriptFile_delete_bash.writeLine("> \"$ERROR_LOG\"");
            outputScriptFile_delete_bash.writeLine("");
            outputScriptFile_delete_bash.writeLine("");
        }
        if (scriptToBuild.contains("upload")){
            outputScriptFile_upload_bash.writeLine("#!/bin/bash");
            outputScriptFile_upload_bash.writeLine("");
            outputScriptFile_upload_bash.writeLine("ERROR_LOG=\""+APPNAME+" - errors.log\"");
            outputScriptFile_upload_bash.writeLine("> \"$ERROR_LOG\"");
            outputScriptFile_upload_bash.writeLine("");
            outputScriptFile_upload_bash.writeLine("");
        }
        
        
        
        //Loop through lines in the input file
        boolean endOfInputFile = false;
        int inputLineCounter = 0;
        while (!endOfInputFile){

            String currLine = affectedFilesList.readLine();
            if (currLine == null){
                //didn't get a line, must be past the end of the file
                endOfInputFile = true;
            }
            else {
                //we did read a line
                inputLineCounter++;


                //take contents of the line, example: Batch_20/Video/004404_Generic 2454.mp4, 
                //and use that to build command(s) we want to add to the output file.
                List<String> newLinesForScriptFile_delete_powershell = new ArrayList<>();
                List<String> newLinesForScriptFile_delete_bash = new ArrayList<>();
                List<String> newLinesForScriptFile_upload_powershell = new ArrayList<>();
                List<String> newLinesForScriptFile_upload_bash = new ArrayList<>();
                
                if (scriptToBuild.contains("delete")){
                    //DELETE COMMAND BUILT HERE
                    
                    //Add line for output to let user know what we're working on
                    String newLine1ForScriptFile_powershell = "Write-Host \"Executing delete for remote file: "+currLine+" \"";
                    //Now add line for the actual command, saved to var
                    String newLine2ForScriptFile_powershell = "$DeleteCommand = \"aws s3 rm `\"s3://"+s3BucketPath+"/"+currLine+"`\" --endpoint-url="+s3EndpointUrl+"\" ";
                    //Output the command so user can see actual command
                    String newLine3ForScriptFile_powershell = "Write-Host \"Command is: $DeleteCommand\"";
                    //Now run the command
                    String newLine4ForScriptFile_powershell = "Invoke-Expression $DeleteCommand";
                    //Output empty line for readability in script
                    String newLine5ForScriptFile_powershell = "Write-Host \"\"";
                    //Add all lines to list, so we can loop to add them to output file (we might do different stuff another time)
                    newLinesForScriptFile_delete_powershell.add(newLine1ForScriptFile_powershell);
                    newLinesForScriptFile_delete_powershell.add(newLine2ForScriptFile_powershell);
                    newLinesForScriptFile_delete_powershell.add(newLine3ForScriptFile_powershell);
                    newLinesForScriptFile_delete_powershell.add(newLine4ForScriptFile_powershell);
                    newLinesForScriptFile_delete_powershell.add(newLine5ForScriptFile_powershell);

                    //Add line for output to let user know what we're working on
                    String newLine1ForScriptFile_bash = "echo \"Executing delete for remote file: "+currLine+"\"";
                    //Now add line for the actual command, saved to var
                    String newLine2ForScriptFile_bash = "CMD=\"aws s3 rm \\\"s3://"+s3BucketPath+"/"+currLine+"\\\" --endpoint-url="+s3EndpointUrl+"\" ";
                    //Output the command so user can see actual command
                    String newLine3ForScriptFile_bash = "echo \"Command is: $CMD\"";
                    //Now run the command
                    String newLine4ForScriptFile_bash = "eval $CMD 2>>\"$ERROR_LOG\"";
                    //Output empty line for readability in script
                    String newLine5ForScriptFile_bash = "echo \"\"";
                    //Add all lines to list, so we can loop to add them to output file (we might do different stuff another time)
                    newLinesForScriptFile_delete_bash.add(newLine1ForScriptFile_bash);
                    newLinesForScriptFile_delete_bash.add(newLine2ForScriptFile_bash);
                    newLinesForScriptFile_delete_bash.add(newLine3ForScriptFile_bash);
                    newLinesForScriptFile_delete_bash.add(newLine4ForScriptFile_bash);
                    newLinesForScriptFile_delete_bash.add(newLine5ForScriptFile_bash);
                    
                    //Add a final line, just an empty line that allows for readability within the script
                    String newLineLastForScriptFile = "";
                    newLinesForScriptFile_delete_powershell.add(newLineLastForScriptFile);
                    newLinesForScriptFile_delete_bash.add(newLineLastForScriptFile);
                    
                    //loop through list and add new lines to output script file(s)
                    for (String newLine : newLinesForScriptFile_delete_powershell) {
                        outputScriptFile_delete_powershell.writeLine(newLine);
                    }
                    for (String newLine : newLinesForScriptFile_delete_bash) {
                        outputScriptFile_delete_bash.writeLine(newLine);
                    }
                    
                }
                
                if (scriptToBuild.contains("upload")){
                    //COPY COMMANDS BUILT HERE
                    
                    //Have to chop up the paths here. Need to isolate the file name from the rest of the path so that
                    //this sync command does not go through testing include/exclude filters for every damn 
                    //file in the entire bucket!!! When I do that, it terminates early and doesn't copy the file.
                    
                    //Files in this affected data list are like:
                    //   Batch_95/Video/230007 Segment Reel 600.mp4
                    //We want to get these isolated like
                    //File name: 230007 Segment Reel 600.mp4
                    //Path: Batch_95/Video/
                    String currLineIsolatedFileName;
                    String currLineIsolatedPath;
                    int indexStartFileName = currLine.lastIndexOf("/")+1;
                    currLineIsolatedFileName = currLine.substring(indexStartFileName, currLine.length());
                    currLineIsolatedPath = currLine.substring(0, indexStartFileName);
                    
                    
                    
                    //Add line for output to let user know what we're working on
                    String newLine1ForScriptFile_powershell = "Write-Host \"Executing upload for: "+currLine+" \"";

                    //Now add line for the actual command, saved to var
                    //sync command is preferable to s3api put-object command because the sync command does not 
                    //take the same amount of time when the file already exists in remote destination location
                    String newLine2ForScriptFile_powershell;
                    newLine2ForScriptFile_powershell = ""
                            + "$UploadCommand = \""
                            + "aws s3 sync "
                            + "`\""+goodBackupFilesPath+currLineIsolatedPath+"`\" "
                            + "`\"s3://"+s3BucketPath+"/"+currLineIsolatedPath+"`\" "
                            + "--exclude '*' "
                            + "--include `\""+currLineIsolatedFileName+"`\" "
                            + "--metadata-directive REPLACE "
                            + "--content-disposition attachment "
                            + "--endpoint-url="+s3EndpointUrl+" "
                            + "--checksum-algorithm=CRC32 "
                            + "\"";

                    //Output the command so user can see actual command
                    String newLine3ForScriptFile_powershell = "Write-Host \"Command is: $UploadCommand\"";
                    //Now run the command
                    String newLine4ForScriptFile_powershell = "Invoke-Expression $UploadCommand";
                    //Output empty line for readability in PowerShell
                    String newLine5ForScriptFile_powershell = "Write-Host \"\"";
                    //Add all lines to list, so we can loop to add them to output file (we might do different stuff another time)
                    newLinesForScriptFile_upload_powershell.add(newLine1ForScriptFile_powershell);
                    newLinesForScriptFile_upload_powershell.add(newLine2ForScriptFile_powershell);
                    newLinesForScriptFile_upload_powershell.add(newLine3ForScriptFile_powershell);
                    newLinesForScriptFile_upload_powershell.add(newLine4ForScriptFile_powershell);
                    newLinesForScriptFile_upload_powershell.add(newLine5ForScriptFile_powershell);
                    
                    
                    

                    //Add line for output to let user know what we're working on
                    String newLine1ForScriptFile_bash = "echo \"Executing upload for file: "+currLine+"\"";

                    //Now add line for the actual command, saved to var
                    //sync command is preferable to s3api put-object command because the sync command does not 
                    //take the same amount of time when the file already exists in remote destination location
                    String newLine2ForScriptFile_bash;
                    newLine2ForScriptFile_bash = ""
                            + "CMD=\""
                            + "aws s3 sync "
                            + "\\\""+goodBackupFilesPath+currLineIsolatedPath+"\\\" "
                            + "\\\"s3://"+s3BucketPath+"/"+currLineIsolatedPath+"\\\" "
                            + "--exclude '*' "
                            + "--include \\\""+currLineIsolatedFileName+"\\\" "
                            + "--metadata-directive REPLACE "
                            + "--content-disposition attachment "
                            + "--endpoint-url="+s3EndpointUrl+" "
                            + "--checksum-algorithm=CRC32\"";
                    
                    //Output the command so user can see actual command
                    String newLine3ForScriptFile_bash = "echo \"Command is: $CMD\"";
                    //Now run the command
                    String newLine4ForScriptFile_bash = "eval $CMD 2>>\"$ERROR_LOG\"";
                    //Output empty line for readability in PowerShell
                    String newLine5ForScriptFile_bash = "echo \"\"";
                    //Add all lines to list, so we can loop to add them to output file (we might do different stuff another time)
                    newLinesForScriptFile_upload_bash.add(newLine1ForScriptFile_bash);
                    newLinesForScriptFile_upload_bash.add(newLine2ForScriptFile_bash);
                    newLinesForScriptFile_upload_bash.add(newLine3ForScriptFile_bash);
                    newLinesForScriptFile_upload_bash.add(newLine4ForScriptFile_bash);
                    newLinesForScriptFile_upload_bash.add(newLine5ForScriptFile_bash);
                    
                    //Add a final line, just an empty line that allows for readability within the script
                    String newLineLastForScriptFile = "";
                    newLinesForScriptFile_upload_powershell.add(newLineLastForScriptFile);
                    newLinesForScriptFile_upload_bash.add(newLineLastForScriptFile);

                    //loop through list and add new lines to output script file(s)
                    for (String newLine : newLinesForScriptFile_upload_powershell) {
                        outputScriptFile_upload_powershell.writeLine(newLine);
                    }
                    for (String newLine : newLinesForScriptFile_upload_bash) {
                        outputScriptFile_upload_bash.writeLine(newLine);
                    }
                    
                }
                
            }

        }//Hit the end of input file
        
        
        
        //Finish up the output script file(s)
        
        //The end is the same for both delete and upload scripts, so compose it
        //first, and then we'll add it to whichever is needed. Of course it's
        //different for PowerShell vs Bash though.
        List<String> newLinesForScriptFile_ending_powershell = new ArrayList<>();
        List<String> newLinesForScriptFile_ending_bash = new ArrayList<>();
            
        //Add lines to output any errors at the end, in case there were any
        //PowerShell stores error records in the $Error automatic variable
        newLinesForScriptFile_ending_powershell.add("Write-Host \"\"");
        newLinesForScriptFile_ending_powershell.add("if ($Error) { ");
        newLinesForScriptFile_ending_powershell.add("  Write-Host \"Errors occurred:\"");
        newLinesForScriptFile_ending_powershell.add("  foreach ($err in $Error) {");
        newLinesForScriptFile_ending_powershell.add("    Write-Host $err.Exception.Message");
        newLinesForScriptFile_ending_powershell.add("  }");
        newLinesForScriptFile_ending_powershell.add("}");
        newLinesForScriptFile_ending_powershell.add("else {");
        newLinesForScriptFile_ending_powershell.add("  Write-Host \"No errors occurred.\"");
        newLinesForScriptFile_ending_powershell.add("}");
        //Also add lines to pause for the user to see results before exiting.
        newLinesForScriptFile_ending_powershell.add("Write-Host \"\"");
        newLinesForScriptFile_ending_powershell.add("Write-Host \"Completed execution of script!\"");
        newLinesForScriptFile_ending_powershell.add("Write-Host \"\"");
        newLinesForScriptFile_ending_powershell.add("Read-Host -Prompt \"Press Enter to exit\"");

        //Add lines to output any errors at the end, in case there were any
        //PowerShell stores error records in the $Error automatic variable
        newLinesForScriptFile_ending_bash.add("if [[ -s \"$ERROR_LOG\" ]]; then");
        newLinesForScriptFile_ending_bash.add("    echo \"Errors occurred:\"");
        newLinesForScriptFile_ending_bash.add("    while IFS= read -r line; do");
        newLinesForScriptFile_ending_bash.add("        echo \"$line\"");
        newLinesForScriptFile_ending_bash.add("    done < \"$ERROR_LOG\"");
        newLinesForScriptFile_ending_bash.add("else");
        newLinesForScriptFile_ending_bash.add("    echo \"No errors occurred.\"");
        newLinesForScriptFile_ending_bash.add("fi");
        //Also add lines to pause for the user to see results before exiting.
        newLinesForScriptFile_ending_bash.add("echo \"\"");
        newLinesForScriptFile_ending_bash.add("echo \"Completed execution of script!\"");
        newLinesForScriptFile_ending_bash.add("echo \"\"");
        newLinesForScriptFile_ending_bash.add("read -p \"Press Enter to exit...\"");
        
        
        //Write these ending lines to the output files needed, and close it/them.
        if (scriptToBuild.contains("delete")) {
            //Loop through list and add new lines to output script file(s)
            for (String newLine : newLinesForScriptFile_ending_powershell) {
                outputScriptFile_delete_powershell.writeLine(newLine);
            }
            outputScriptFile_delete_powershell.closeWriting();
            for (String newLine : newLinesForScriptFile_ending_bash) {
                outputScriptFile_delete_bash.writeLine(newLine);
            }
            outputScriptFile_delete_bash.closeWriting();
        }
        if (scriptToBuild.contains("upload")) {
            //Loop through list and add new lines to output script file(s)
            for (String newLine : newLinesForScriptFile_ending_powershell) {
                outputScriptFile_upload_powershell.writeLine(newLine);
            }
            outputScriptFile_upload_powershell.closeWriting();
            for (String newLine : newLinesForScriptFile_ending_bash) {
                outputScriptFile_upload_bash.writeLine(newLine);
            }
            outputScriptFile_upload_bash.closeWriting();
        }

        //Close both input file
        affectedFilesList.closeReading();
        
        System.out.println("Finished");
        
    }
}
