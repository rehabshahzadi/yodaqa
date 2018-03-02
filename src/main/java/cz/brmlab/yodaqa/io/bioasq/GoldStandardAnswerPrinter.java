package cz.brmlab.yodaqa.io.bioasq;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import cz.brmlab.yodaqa.flow.dashboard.Question;
import cz.brmlab.yodaqa.flow.dashboard.QuestionDashboard;
import cz.brmlab.yodaqa.model.AnswerHitlist.Answer;
import cz.brmlab.yodaqa.model.Question.GSAnswer;
import cz.brmlab.yodaqa.model.Question.QuestionInfo;

/**
 * A consumer that displays the top answers in context of the asked question and
 * expected true answer provided as gold standard.
 *
 * Pair this with BioASQQuestionReader. Note that this *IS NOT* JSON output
 * expected by BioASQ submission system; that will be done by postprocessing.
 *
 * The output format is, tab separated ID TIME QUESTION SCORE RANK NRANKS
 * ANSWERPATTERN CORRECTANSWER TOPANSWERS... where TIME is the processing time
 * in seconds (fractional), SCORE is (1.0 - log(correctrank)/log(#answers)),
 * RANK is the corretrank and NRANKS is #answers. ANSWERPATTERN is the *first*
 * of the specified correct answers.
 *
 * XXX how to represent list-type answers?
 */

public class GoldStandardAnswerPrinter extends JCasConsumer_ImplBase {
	/**
	 * Number of top answers to show.
	 */
	public static final String PARAM_TOPLISTLEN = "TOPLISTLEN";
	@ConfigurationParameter(name = PARAM_TOPLISTLEN, mandatory = false, defaultValue = "2")
	private int topListLen;

	/**
	 * Number of top answers to show.
	 */
	public static final String PARAM_TSVFILE = "TSVFILE";
	@ConfigurationParameter(name = PARAM_TSVFILE, mandatory = true)
	private String TSVFile;
	PrintWriter TSVOutput;

	JSONArray results = new JSONArray();
	LinkedList<JSONObject> questions = new LinkedList<>();

	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);

		JSONParser parser = new JSONParser();
		JSONObject data;
		try {
			data = (JSONObject) parser.parse(new FileReader(TSVFile));
		} catch (Exception io) {
			throw new ResourceInitializationException(io);
		}
		for (Object o : (JSONArray) data.get("questions")) {
			questions.add((JSONObject) o);
		}
	}

	public static boolean isCorrectAnswer(String text, Collection<GSAnswer> gs) {
		for (GSAnswer gsa : gs) {
			StringArray sa = gsa.getTexts();
			for (String s : sa.toStringArray()) {
				if (s.toLowerCase().equals(text.toLowerCase())) {
					return true;
				}
			}
		}
		return false;
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		JCas answerHitlist;
		try {
			answerHitlist = jcas.getView("AnswerHitlist");
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}
		FSIndex idx = answerHitlist.getJFSIndexRepository().getIndex("SortedAnswers");
		FSIterator answers = idx.iterator();
		JSONObject question = questions.remove();
		ArrayList<String> toplist = new ArrayList();
		if (answers.hasNext()) {

			int i = 0;
			while (answers.hasNext()) {
				Answer answer = (Answer) answers.next();
				String text = answer.getText();

				if (i < topListLen) {
					toplist.add(text);
					i++;
				}
			}
			JSONObject questionResult = getQuestionResult(question, toplist);
			results.add(questionResult);
			BioASQFileWriter.writeSubmissionFile(results, "test");
		}
		

	}

	private JSONObject getQuestionResult(JSONObject question, ArrayList<String> answers) {
		JSONObject questionResult = null;
		ArrayList<JSONObject> sData = new ArrayList();
		 ArrayList<ArrayList<String>> temp = new ArrayList<ArrayList<String>>();
	
		for (Object s : (JSONArray) question.get("snippets")) {
			sData.add((JSONObject) s);
		}
		
	    Object dData=(JSONArray)question.get("documents");
		

		questionResult = new JSONObject();
		
		String questionBody = (String) question.get("body");
		String questionId = (String) question.get("id");
		String questionType = (String) question.get("type");
		questionResult.put("id", questionId);
		questionResult.put("type", questionType);
		questionResult.put("body", questionBody);
		questionResult.put("snippets", sData);
		questionResult.put("documents", dData);
		if (questionType.toLowerCase().equals("yesno")) {
			questionResult.put("ideal_answer", "yes");
			questionResult.put("exact_answer", "yes");

		}
		if (questionType.toLowerCase().equals("summary")) {
			// used bm25 for sentence similarity

			questionResult.put("ideal_answer", answers.get(0));
		}
		if (questionType.toLowerCase().equals("factoid") | questionType.toLowerCase().equals("list")) 
		{

			for(String s: answers)
			{	ArrayList<String> idealData = new ArrayList();
				idealData.add(s);
				temp.add(idealData);
			}
			questionResult.put("ideal_answer", answers.get(0));
			
            questionResult.put("exact_answer", temp);

		}

		return questionResult;

	}

}
