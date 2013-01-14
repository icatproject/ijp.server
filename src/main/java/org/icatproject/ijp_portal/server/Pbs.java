package org.icatproject.ijp_portal.server;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import uk.ac.rl.esc.catutils.CheckedProperties;
import uk.ac.rl.esc.catutils.CheckedProperties.CheckedPropertyException;
import uk.ac.rl.esc.catutils.ShellCommand;
import org.icatproject.ijp_portal.shared.Constants;
import org.icatproject.ijp_portal.shared.ServerException;

public class Pbs {

	final static Logger logger = LoggerFactory.getLogger(Pbs.class);

	class PbsParser extends DefaultHandler {

		private Map<String, String> states = new HashMap<String, String>();
		private Map<String, String> jobs = new HashMap<String, String>();
		private String host;
		private String state;
		private String job;
		private StringBuilder text = new StringBuilder();

		void reset() {
			host = null;
			state = null;
			job = null;
			text.setLength(0);
			states.clear();
			jobs.clear();
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {
			text.setLength(0);
			if (qName.equals("Node")) {
				host = null;
				state = null;
				job = null;
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if (qName.equals("Node") && host != null) {
				if (state != null) {
					states.put(host, state);
				}
				if (job != null) {
					jobs.put(host, job);
				}
			} else if (qName.equals("name")) {
				host = text.toString();
			} else if (qName.equals("state")) {
				state = text.toString();
			} else if (qName.equals("jobs")) {
				job = text.toString();
			}
		}

		@Override
		public void characters(char ch[], int start, int length) throws SAXException {
			text.append(ch, start, length);
		}
	}

	private PbsParser pbsParser;
	private XMLReader xmlReader;
	private String pbsnodes;
	private String qsig;
	private String qstat;
	private String qsub;

	public Pbs() throws ServerException {

		CheckedProperties props = new CheckedProperties();
		try {
			props.loadFromFile(Constants.PROPERTIES_FILEPATH);
			pbsnodes = props.getString("pbsnodes");
			qsig = props.getString("qsig");
			qstat = props.getString("qstat");
			qsub = props.getString("qsub");
		} catch (CheckedPropertyException e) {
			throw new ServerException("CheckedPropertyException " + e.getMessage());
		}

		pbsParser = new PbsParser();
		SAXParserFactory saxFactory = SAXParserFactory.newInstance();

		try {
			xmlReader = saxFactory.newSAXParser().getXMLReader();
		} catch (SAXException e) {
			throw new ServerException("SAX Exception " + e.getMessage());
		} catch (ParserConfigurationException e) {
			throw new ServerException("SAX Parser Configuration Exception " + e.getMessage());
		}
		this.xmlReader.setContentHandler(pbsParser);
		this.xmlReader.setErrorHandler(pbsParser);

	}

	public Map<String, String> getStates() throws ServerException {
		parse();
		/*
		 * A copy of the map is returned so that subsequent calls to setOffline or setOnline - both
		 * of which call parse - don't mess up the map.
		 */
		return new HashMap<String, String>(pbsParser.states);
	}

	private void parse() throws ServerException {
		ShellCommand sc = new ShellCommand(pbsnodes, "-x");
		if (sc.isError()) {
			throw new ServerException("Code " + sc.getExitValue() + ": " + sc.getStderr());
		}
		pbsParser.reset();
		try {
			xmlReader.parse(new InputSource(new StringReader(sc.getStdout())));
		} catch (Exception e) {
			throw new ServerException(e.getMessage());
		}
	}

	private Map<String, String> getJobs() throws ServerException {
		parse();
		return pbsParser.jobs;
	}

	public void setOffline(String hostName) throws ServerException {
		ShellCommand sc = new ShellCommand(pbsnodes, "-o", hostName);
		if (sc.isError()) {
			throw new ServerException("Code " + sc.getExitValue() + ": " + sc.getStderr());
		}
		String jobs = getJobs().get(hostName);
		if (jobs != null) {
			for (String job : jobs.split(",")) {
				suspendJobs(job.split("/")[1]);
			}
		}
		logger.debug(hostName + " is now offline");
	}

	public void setOnline(String hostName) throws ServerException {
		ShellCommand sc = new ShellCommand(pbsnodes, "-c", hostName);
		if (sc.isError()) {
			throw new ServerException(sc.getMessage());
		}
		sc = new ShellCommand(qstat, "-n1");
		if (sc.isError()) {
			throw new ServerException(sc.getMessage());
		}
		for (String line : sc.getStdout().split("(?m)^")) {
			String[] words = line.split("\\s+");
			if (words.length == 12 && hostName.startsWith(words[11] + ".")) {
				resumeJobs(words[0]);
			}
		}
		sc = new ShellCommand(qsub, "-o", "/dev/null", "-e", "/dev/null", "wakeup.sh");
		if (sc.isError()) {
			throw new ServerException(sc.getMessage());
		}
		if (sc.getStdout() != null) {
			logger.debug("qsub reports " + sc.getStdout());
		}
		logger.debug(hostName + " is now online");
	}

	private void resumeJobs(String jobName) throws ServerException {
		ShellCommand sc = new ShellCommand(qsig, "-s", "resume", jobName);
		if (sc.isError()) {
			throw new ServerException(sc.getMessage());
		}
		logger.debug(jobName + " has now resumed");
	}

	private void suspendJobs(String jobName) throws ServerException {
		ShellCommand sc = new ShellCommand(qsig, "-s", "suspend", jobName);
		if (sc.getExitValue() == 170) { // Invalid state for job
			logger.debug(sc.getMessage());
			return;
		}
		if (sc.isError()) {
			throw new ServerException(sc.getMessage());
		}
		logger.debug(jobName + " is now suspended");
	}

}
