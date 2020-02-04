import java.time.Instant

import groovy.json.JsonSlurper
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import groovy.swing.SwingBuilder
import java.awt.*
import javax.swing.*
import javax.swing.event.*


String apiCall(String request) {
    String user = "admin"
    String password = "admin"
    
    String auth = "Basic " + (user + ':' + password).bytes.encodeBase64();
    
    URL url = new URL( request );
    
    HttpURLConnection conn= (HttpURLConnection) url.openConnection();           
    conn.setDoOutput( true );
    conn.setInstanceFollowRedirects( false );
    conn.setRequestMethod("GET");
    conn.setRequestProperty("Content-Type", "application/json"); 
    conn.setRequestProperty("Authorization", auth);
    
    conn.setUseCaches( false );
    
    try {
        //println("Jenkins - Trying to get: " + request);
        int responseCode = conn.getResponseCode();
        BufferedReader i1 = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();
        
        while ((inputLine = i1.readLine()) != null) {
            response.append(inputLine);
        }
        i1.close();
        //println("Jenkins - Success");
        return response.toString();
    } catch (Exception ex) {
        println("Jenkins - Fail: " + ex.toString());
        return "";
    }
}

void showFailed (String name, String url, def t) {

    SwingBuilder swing = new SwingBuilder()

    def panel = {

      JEditorPane editorPane = swing.editorPane()
      editorPane.setContentType("text/html")
      editorPane.setText("<h1>Job failed</h1> <a href='" + url +"'>" + name + "</a> on " + t)
      editorPane.setEditable(false);
      editorPane.setOpaque(false);
      editorPane.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent hle) {
              if (HyperlinkEvent.EventType.ACTIVATED.equals(hle.getEventType())) {
                Desktop.getDesktop().browse(hle.getURL().toURI());
              }
            }
          });
    }

    swing.edt {
      frame(title:'Build Failed!', size:[500, 200], defaultCloseOperation:JFrame.DISPOSE_ON_CLOSE, show:true) { panel() }
    }

}

String host = "yourHost"
String port = "yourPort"

def ts
def allJobs

ts = java.lang.System.currentTimeMillis() - 7200000

def jsonSlurper = new JsonSlurper()

while (true) {
	allJobs = jsonSlurper.parseText(apiCall("http://" + host + ":" + port + "/api/json?tree=jobs[name,displayName,url,color,lastBuild[number,duration,timestamp]]"))

	failed = allJobs.jobs.findAll {it.color=="red" && (it.lastBuild.duration + it.lastBuild.timestamp >= ts)}

	println ("There are " + failed.size() + " recently (1hr) failed jobs")

	if (failed.size() > 0) {

		failed.each { 
			
			Date t = Date.from(Instant.ofEpochSecond((long)((it.lastBuild.timestamp + it.lastBuild.duration) / 1000)))
			showFailed(it.displayName, it.name, t.toString())
		}
		
	}

	sleep (120000)
	ts = java.lang.System.currentTimeMillis()
}