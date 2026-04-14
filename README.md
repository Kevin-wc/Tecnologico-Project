# Tecnologico-Project (Right Triangle Counter P3)

## Team Information
- **University:** Belmont University
- **Course:** CSC 4180 - Operating Systems
- **Project:** Program 3 - PointStore & Triangle Counter
- **Team Members:**
  - Kevin Gonzalez
  - Elle Simonds
  - Eli Mizerski
    
---

## Collaboration
We are super excited to collaborate with our paired TEC group! We look forward to working together. Please feel free to reach out if you have any questions or encounter issues.

---
## Program
This program counts the number of **right triangles** that can be formed from a set of 2D integer points.

It supports:
- **Text input files** (P0 format)
- **Binary input files** (`.dat`, using memory-mapped I/O)

- This project includes:
- Single-threaded version
- Multi-threaded version
- Multi-process version

All point access is handled through a shared interface (`PointStore`) to ensure a clean design

---
## Project Structure
## Project Structure

```text
com/tryright/
├── Triangles.java
├── ThreadTriangles.java
├── ProcessTriangles.java
├── TriangleUtil.java
├── PointStore.java
├── TextPointStore.java
├── BinPointStore.java

test/
├── testplan.pdf
├── performance.pdf
├── scaletest.txt
├── sample1.txt
├── small_valid.txt
├── eight_points.txt
├── many_points.txt
├── missing_points.txt
├── noread.txt
```
---
## Requirements
- Java JDK 8 or higher
- Terminal / Command line

--- 
## Running on Ubuntu VM
### 1. Install Java (if not already installed)
```bash
sudo apt update
sudo apt install openjdk-17-jdk
```
### 2. Download Repository
```bash
git clone <repo-link>
cd <repo-folder>
```
Or upload and unzip:
```bash
unzip project.zip
cd project
```
### 3. Compile the Project
```bash
javac com/tryright/*.java
```
### 4. Run the Program
#### Single-threaded
```bash
java com.tryright.Triangles <inputfile>
java com.tryright.Triangles test/sample1.txt
```
#### Multi-threaded
```bash
java com.tryright.ThreadTriangles <inputfile> <numThreads>
java com.tryright.ThreadTriangles test/sample1.txt 4
```
#### Multi-process
```bash
java com.tryright.ProcessTriangles <inputfile> <numProcesses>
java com.tryright.ProcessTriangles test/sample1.txt 4
```
## Input File Format

### Text Format
The file begins with an integer `N`, forllowing by `N` pairs of integers that represent points:
```text
N
x1 y1
x2 y2
...
xN yN
```
### Binary Format (`.dat`)
- Each point consists of:
  - 4-byte integer (x)
  - 4-byte integer (y)
- Stored in **big-endian** format
- Each point = **8 bytes total**
- File size must be a multiple of 8 bytes

Example (hex representation of point `(1, 2)`:
```text
00 00 00 01 00 00 00 02
```
--- 
## Test Cases & Expected Output

| File Name            | Description                         | Expected Output |
|---------------------|-------------------------------------|-----------------|
| `sample1.txt`       | Assignment sample case              | 4               |
| `small_valid.txt`   | Simple right triangle               | 1               |
| `eight_points.txt`  | 8-point dataset                     | 16              |
| `many_points.txt`   | Points along x and y axes           | 2500            |
| `scaletest.txt`     | Large dataset (performance test)    | 6250000         |
| `missing_points.txt`| Invalid format (missing data)       | Error           |
| `noread.txt`        | File permission issue               | Error           |

---
## Error Handling
- Invalid file format -> error to stderr
- Missing file -> error to stderr
- Incorrect arguments -> usage message

Output is always:
- One number (success), OR
- Error message (failure)
---
## Performance Notes
- Time complexity: **O(n³)**
- Optimizations:
  - Preloading points into arrays
  - Squared distance math
- Threads improve perfomance significantly
- Processes show overhead due to separate memory
- Uses `PointStore` interface for abstraction
- Supports text + binary
- Uses memory-mapped I/O for binary files
---
## For TEC Group
if anything does not work, please reach out via our emails!
