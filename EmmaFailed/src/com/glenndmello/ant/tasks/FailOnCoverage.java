package com.glenndmello.ant.tasks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Usage
 * 
 * <taskdef name="coverageFailTask" classpath="path/to/FailBuildOnCoverage.jar" classname="com.galiam.ant.tasks.FailOnCoverage" />
 * <coverageFailTask coverageFile="$path/to/report/emma/coverage.txt" failureProperty="coverage.failed" failOnError="false" 
 *                   classThreshold = "28" methodThreshold = "22" blockThreshold="20" lineThreshold="18" />
 * 
 * @author Glenn D'mello
 * 
 */
public class FailOnCoverage extends Task {

    private String coverageFile;
    private String failureProperty;
    private boolean failOnError;
    private int classThreshold;
    private int methodThreshold;
    private int blockThreshold;
    private int lineThreshold;

    public void execute() {
        System.out.println("Started checking coverage..");
        String failureString = "";
        boolean failed = false;
        if (coverageFile == null) {
            System.err.println("Requires full path to EMMA coverage txt report.");
            return;
        }
        if (failureProperty == null) {
            System.err.println("Requires 'failureProperty' to be set.");
            return;
        }
        File f = new File(coverageFile);
        try {
            FileReader fr = new FileReader(f);
            BufferedReader br = new BufferedReader(fr);
            String ln;
            while ((ln = br.readLine()) != null) {
                if (ln.startsWith("[class, %]")) {
                    ln = br.readLine();
                    break;
                }
            }
            br.close();
            fr.close();
            // ln is of the form: 28% (52/184)! 22% (378/1720)! 20%
            // (7700/38446)! 18% (1382.5/7553)! all classes
            int classActual = 0;
            int methodActual = 0;
            int blockActual = 0;
            int lineActual = 0;
            String classS = "";
            String methodS = "";
            String blockS = "";
            String lineS = "";

            if (ln != null && ln.length() > 30) {
                String parts[] = ln.split("!");
                classS = parts[0].trim();
                methodS = parts[1].trim();
                blockS = parts[2].trim();
                lineS = parts[3].trim();

                classActual = Integer.parseInt(classS.substring(0, classS.indexOf("%")).trim());
                methodActual = Integer.parseInt(methodS.substring(0, methodS.indexOf("%")).trim());
                blockActual = Integer.parseInt(blockS.substring(0, blockS.indexOf("%")).trim());
                lineActual = Integer.parseInt(lineS.substring(0, lineS.indexOf("%")).trim());
            } else {
                System.out.println("Invalid line read from report file. Has the format changed? " + ln);
            }
            if (classActual < classThreshold) {
                failed = true;
                failureString = "   Failed class coverage threshold.  Required: " + classThreshold + ", actual: " + classS
                            + System.getProperty("line.separator");
            }
            if (methodActual < methodThreshold) {
                failed = true;
                failureString += "   Failed method coverage threshold. Required: " + methodThreshold + ", actual: " + methodS
                            + System.getProperty("line.separator");
            }
            if (blockActual < blockThreshold) {
                failed = true;
                failureString += "   Failed block coverage threshold.  Required: " + blockThreshold + ", actual: " + blockS
                            + System.getProperty("line.separator");
            }
            if (lineActual < lineThreshold) {
                failed = true;
                failureString += "   Failed line coverage threshold.   Required: " + lineThreshold + ", actual: " + lineS
                            + System.getProperty("line.separator");
            }

        } catch (FileNotFoundException e) {
            failureString = "Coverage results file not found " + coverageFile + ": " + e.getMessage();
        } catch (IOException e) {
            failureString = "Error reading coverage file " + coverageFile + ": " + e.getMessage();
        }

        if (failed) {
            if (!failOnError) {
                if (getProject() != null) {
                    getProject().setNewProperty(failureProperty, failureString);
                } else {
                    System.err.println("Project is null, cannot set failure property for failed coverage: " + failureString);
                }
            } else {
                throw new BuildException("Code coverage failed: " + System.getProperty("line.separator") + failureString);
            }
        }
        if (failed) {
            System.out.println("Coverage failed threshold check:" + System.getProperty("line.separator") + failureString);
        } else {
            System.out.println("Test coverage passed threshold checks.");
        }
        System.out.println("Done checking coverage..");
    }

    public void setCoverageFile(String coverageFile) {
        this.coverageFile = coverageFile;
    }

    public void setFailureProperty(String prop) {
        this.failureProperty = prop;
    }

    public void setFailOnError(boolean shouldFail) {
        this.failOnError = shouldFail;
    }

    public void setClassThreshold(int classTh) {
        this.classThreshold = classTh;
    }

    public void setMethodThreshold(int methodTh) {
        this.methodThreshold = methodTh;
    }

    public void setBlockThreshold(int blockTh) {
        this.blockThreshold = blockTh;
    }

    public void setLineThreshold(int lineTh) {
        this.lineThreshold = lineTh;
    }

    public static void main(String... args) {
        FailOnCoverage fc = new FailOnCoverage();
        fc.setCoverageFile("coverage.txt");
        fc.setClassThreshold(40);
        fc.setMethodThreshold(40);
        fc.setBlockThreshold(40);
        fc.setLineThreshold(40);
        fc.setFailOnError(true);
        fc.execute();
    }
}
