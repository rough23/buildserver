package cz.utb.fai.cudaonlineide.buildserver;

/**
 * Class with all constants used in this project.
 * 
 * @author Belanec
 * 
 */
public class BuildServerConstants {

	public static final String URL = "http://195.178.90.55:8080/mdp/api/submissions";
	public static final String UPLOAD = "?lang=CUDA";
	public static final String EXECUTE = "/execute";
	public static final String ARGUMENTS = "arguments";
	
	public static final String CONTENT_TYPE = "Content-Type";
	public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
	
	public static final String UUID = "uuid";
	public static final String COMPILATION_OUTPUT = "compilationOutput";
	public static final String EXECUTION_OUTPUT = "executionOutput";
	public static final String COMPILATION_FAILED = "compilationFailed";
}