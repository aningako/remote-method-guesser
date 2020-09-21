package de.qtc.rmg.utils;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import de.qtc.rmg.io.Logger;

public class ClassWriter {

    public String templateFolder;
    public String sourceFolder;

    private String template;
    private String sampleClassName;


    public ClassWriter(String templateFolder, String sourceFolder) {
        this.templateFolder = templateFolder;
        this.sourceFolder = sourceFolder;
    }


    public File[] getTemplateFiles() {

        File templateDir = new File(this.templateFolder);
        File[] containedFiles = templateDir.listFiles();

        List<File> templateFiles = new ArrayList<File>();
        for( File file : containedFiles ) {
            if( file.getName().matches("[a-zA-Z0-9]+Template.java") && ! file.getName().equals("SampleTemplate.java") ) {
                templateFiles.add(file);
            }
        }

        return templateFiles.toArray(new File[templateFiles.size()]);
    }


    public void loadTemplate(String templateName) {

        String path = this.templateFolder + "/" + templateName;
        File sampleTemplate = new File(path);

        if( !sampleTemplate.exists() ) {
            Logger.eprintln("Error: '" + templateName + "' seems not to be contained in '" + this.templateFolder + "'.");
            Logger.eprintln("Stopping execution.");
            System.exit(1);
        }

        Logger.print("Reading template file: '" + path +  "'... ");
        try {

            this.template = new String(Files.readAllBytes(Paths.get(path)));
            Logger.printlnPlain("done");

        } catch( Exception e ) {

            Logger.printlnPlain("failed");
            Logger.eprintln("Error: unable to read template file");
            System.exit(1);
        }
    }


    public String writeClass(String fullClassName) throws UnexpectedCharacterException{

        Security.checkPackageName(fullClassName);

        String[] components = splitNames(fullClassName);
        String packageName = components[0];
        String className = components[1];

        this.template = this.template.replace("<PACKAGENAME>", packageName);
        this.template = this.template.replace("<CLASSNAME>", className);

        String destination = this.sourceFolder + "/" + className + ".java";
        Logger.print("Writing class '" + destination + "' to disk... ");

        try {

            PrintWriter writer = new PrintWriter(destination, "UTF-8");
            writer.print(this.template);
            writer.close();
            Logger.printlnPlain("done.");

        } catch( Exception e ) {

            Logger.printlnPlain("failed.");
            Logger.eprintln("Error: Cannot open '" + destination + "'");
            System.exit(1);

        }

        return destination;
    }


    public void prepareSample(String packageName, String className, String boundName, Method method, String sampleClassName, String remoteHost, int remotePort) throws UnexpectedCharacterException{

        Security.checkAlphaNumeric(className);
        Security.checkAlphaNumeric(boundName);
        Security.checkPackageName(packageName);
        Security.checkAlphaNumeric(sampleClassName);

        this.loadTemplate("SampleTemplate.java");
        this.sampleClassName = sampleClassName;
        String port = String.valueOf(remotePort);

        int numberOfArguments = method.getParameterCount();
        StringBuilder argumentString = new StringBuilder();

        Class<?>[] typeOfArguments = method.getParameterTypes();

        for(int ctr = 1; ctr <= numberOfArguments; ctr++) {
            if( typeOfArguments[ctr-1].isArray() ) {
                argumentString.append("convertToArray(argv[" + ctr + "])" + ((ctr == numberOfArguments) ? "" : ","));
            } else {
                argumentString.append("argv[" + ctr + "]" + ((ctr == numberOfArguments) ? "" : ","));
            }
        }

        Logger.print("Preparing sample... ");

        this.template = this.template.replace(  "<PACKAGE>",      packageName + "." + className);
        this.template = this.template.replace(  "<CLASSNAME>",    sampleClassName);
        this.template = this.template.replace(  "<METHODSIG>",    method.toString());
        this.template = this.template.replace(  "<REMOTEHOST>",   remoteHost);
        this.template = this.template.replace(  "<REMOTEPORT>",   port);
        this.template = this.template.replace(  "<BOUNDNAME>",    boundName);
        this.template = this.template.replace(  "<CLASS>",        className);
        this.template = this.template.replace(  "<METHODNAME>",   method.getName());
        this.template = this.template.replace(  "<RETURNTYPE>",   method.getReturnType().getName());
        this.template = this.template.replace(  "<ARGCOUNT>",     Integer.toString(numberOfArguments));
        this.template = this.template.replace(  "<ARGUMENTS>",    argumentString.toString());

        Logger.printlnPlain("done.");
    }


    public String writeSample() {

        String destination = this.sourceFolder + "/" + this.sampleClassName + ".java";
        Logger.print("Writing sample '" + destination + "' to disk... ");

        try {

            PrintWriter writer = new PrintWriter(destination, "UTF-8");
            writer.print(template);
            writer.close();
            Logger.printlnPlain("done.");

        } catch( Exception e ) {

            Logger.printlnPlain("failed.");
            Logger.eprintln("Error: Cannot open '" + destination + "'");
            System.exit(1);
        }

        return destination;
    }


    public static String[] splitNames(String fullName) {
        String className = fullName.substring(fullName.lastIndexOf(".") + 1, fullName.length());
        String packageName = fullName.substring(0, fullName.lastIndexOf("."));
        return new String[] { packageName, className };
    }
}
