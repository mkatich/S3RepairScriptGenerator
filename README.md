# **S3RepairScriptGenerator**

## **Overview**
Generates PowerShell and Bash scripts to repair specific corrupted, missing, or unreadable objects in S3‑compatible storage. Automates deletion of bad files and re‑upload of known‑good backups using user configurable CLI parameters.

---

## **Why This Tool Exists**
A storage incident at an S3‑compatible provider caused a subset of objects in our bucket to become corrupted, partially unreadable, or completely inaccessible. Initial symptoms were subtle:

- Large files (3–8 GB) consistently failed to download around the same byte offset  
- Multiple users across different ISPs reproduced the same failures, ruling out network issues
- Web browsers, Cyberduck, S3 Browser, and AWS CLI all failed  
- Multipart download tools showed one specific part failing repeatedly  

This behavior did not match normal packet loss or client‑side issues. After investigation, the storage provider disclosed that a hardware/software failure had caused a number of objects to become inaccessible or corrupted. The only remedy for us was that affected objects needed to be deleted and replaced with clean versions from our backups.

The storage provider was able to provide a list of affected objects - about 2,000 of them. Due to the large number of affected objects, and the fact most of them were quite large and would require long upload times, we needed a way to automate the repair.

**S3RepairScriptGenerator was created to automate the repair workflow.**

---

## **What This Tool Does**

### **1. Delete Scripts**
Generates PowerShell and Bash scripts that remove corrupted objects:

- Uses your specified bucket  
- Uses your specified S3 endpoint  
- Deletes each affected object, as specified in the list  

### **2. Upload Scripts**
Generates PowerShell and Bash scripts that re‑upload clean backup files:

- Maps each affected key to a local backup file  
- Uploads using AWS CLI  
- Supports optional `--content-disposition` metadata for HTTP header  
- Works with any S3‑compatible provider  

### **3. Fully Configurable CLI**
You control:

- Affected files list  
- Script type (delete, upload, both)  
- S3 Bucket name  
- S3 Endpoint URL  
- Backup directory  
- Output directory  
- Optional content‑disposition  

---

## **Installation**
Compile the project using your preferred Java build tool, then run the JAR:

```bash
java -jar S3RepairScriptGenerator.jar [parameters...]
```

Requires **Java 17+**.

---

## **Usage**

Recommended parameter order:

```
--affected
--script
--bucket
--endpoint
--backup
--content-disposition
--test-mode
--outdir
```

### **Delete only**
```bash
java -jar S3RepairScriptGenerator.jar \
    --affected "C:/lists/Affected.txt" \
    --script delete \
    --bucket datarecovery \
    --endpoint https://s3.example.com \
    --outdir "C:/output/"
```

### **Upload only**
```bash
java -jar S3RepairScriptGenerator.jar \
    --affected "C:/lists/Affected.txt" \
    --script upload \
    --bucket datarecovery \
    --endpoint https://s3.example.com \
    --backup "Z:/" \
    --content-disposition attachment \
    --outdir "C:/output/"
```

### **Both delete + upload**
```bash
java -jar S3RepairScriptGenerator.jar \
    --affected "C:/lists/Affected.txt" \
    --script both \
    --bucket datarecovery \
    --endpoint https://s3.example.com \
    --backup "Z:/" \
    --outdir "C:/output/"
```

---

## **Parameter Reference**

| Parameter | Required | Description |
|----------|----------|-------------|
| `--affected` | Yes | Path to text file listing affected object keys (one per line). |
| `--script` | Yes | `delete`, `upload`, or `both`. |
| `--bucket` | Yes | S3 bucket name (no `s3://` prefix). |
| `--endpoint` | Yes | S3 endpoint URL for your storage provider. |
| `--backup` | Required for upload/both | Directory containing known‑good backup files. |
| `--content-disposition` | Optional | Adds `--content-disposition` to upload commands. |
| `--test-mode` | Optional | Generates scripts that echo commands instead of executing them. |
| `--outdir` | Yes | Directory where generated scripts will be written. |

---

## **How It Works Internally**
1. Reads the affected file list  
2. Normalizes paths and validates parameters  
3. Generates PowerShell and Bash scripts  
4. Ensures clean overwrite of script files  

The generated scripts are crafted in a way so that they can be re-ran multiple times to be sure that they have completed all the actions. The "aws s3 sync" command is used for the upload, and it skips quickly if the object already exists. The "aws s3api put-object" command on the other hand would still take the time to upload all the data. This is helpful particularly with large files and/or a large number of files.