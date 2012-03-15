package com.github.browep.oauthtwitter;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import com.sugree.twitter.DialogError;
import com.sugree.twitter.TwDialog;
import com.sugree.twitter.Twitter;
import com.sugree.twitter.TwitterError;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Main extends Activity
{
    public static final String TAG =  Main.class.getSimpleName();
    public static final String TWITTER_OAUTH_REQUEST_TOKEN_ENDPOINT = "http://twitter.com/oauth/request_token";
    public static final String TWITTER_OAUTH_ACCESS_TOKEN_ENDPOINT = "http://twitter.com/oauth/access_token";
    public static final String TWITTER_OAUTH_AUTHORIZE_ENDPOINT = "http://twitter.com/oauth/authorize";
    private CommonsHttpOAuthProvider commonsHttpOAuthProvider;
    private CommonsHttpOAuthConsumer commonsHttpOAuthConsumer;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        commonsHttpOAuthProvider = new CommonsHttpOAuthProvider(TWITTER_OAUTH_REQUEST_TOKEN_ENDPOINT,
                TWITTER_OAUTH_ACCESS_TOKEN_ENDPOINT, TWITTER_OAUTH_AUTHORIZE_ENDPOINT);
        commonsHttpOAuthConsumer = new CommonsHttpOAuthConsumer(getString(R.string.twitter_oauth_consumer_key),
                getString(R.string.twitter_oauth_consumer_secret));
        commonsHttpOAuthProvider.setOAuth10a(true);
        TwDialog dialog = new TwDialog(this, commonsHttpOAuthProvider, commonsHttpOAuthConsumer,
                dialogListener, R.drawable.android);
        dialog.show();

    }


    private Twitter.DialogListener dialogListener = new Twitter.DialogListener() {
        public void onComplete(Bundle values) {
            String secretToken = values.getString("secret_token");
            Log.i(TAG,"secret_token=" + secretToken);
            String accessToken = values.getString("access_token");
            Log.i(TAG,"access_token=" + accessToken);
            new Tweeter(accessToken,secretToken).tweet(
                    "Tweet from sample Android OAuth app.  unique code: " + System.currentTimeMillis());
        }

        public void onTwitterError(TwitterError e) { Log.e(TAG,"onTwitterError called for TwitterDialog",
                new Exception(e)); }

        public void onError(DialogError e) { Log.e(TAG,"onError called for TwitterDialog", new Exception(e)); }

        public void onCancel() { Log.e(TAG,"onCancel"); }
    };

    public static final Pattern ID_PATTERN = Pattern.compile(".*?\"id_str\":\"(\\d*)\".*");
    public static final Pattern SCREEN_NAME_PATTERN = Pattern.compile(".*?\"screen_name\":\"([^\"]*).*");

    public class Tweeter {
        protected CommonsHttpOAuthConsumer oAuthConsumer;

        public Tweeter(String accessToken, String secretToken) {
            oAuthConsumer = new CommonsHttpOAuthConsumer(getString(R.string.twitter_oauth_consumer_key),
                    getString(R.string.twitter_oauth_consumer_secret));
            oAuthConsumer.setTokenWithSecret(accessToken, secretToken);
        }

        public boolean tweet(String message) {
            if (message == null && message.length() > 140) {
                throw new IllegalArgumentException("message cannot be null and must be less than 140 chars");
            }
            // create a request that requires authentication

            try {
                HttpClient httpClient = new DefaultHttpClient();
                Uri.Builder builder = new Uri.Builder();
                builder.appendPath("statuses").appendPath("update.json")
                        .appendQueryParameter("status", message);
                Uri man = builder.build();
                HttpPost post = new HttpPost("http://twitter.com" + man.toString());
                oAuthConsumer.sign(post);
                HttpResponse resp = httpClient.execute(post);
                String jsonResponseStr = convertStreamToString(resp.getEntity().getContent());
                Log.i(TAG,"response: " + jsonResponseStr);
                String id = getFirstMatch(ID_PATTERN,jsonResponseStr);
                Log.i(TAG,"id: " + id);
                String screenName = getFirstMatch(SCREEN_NAME_PATTERN,jsonResponseStr);
                Log.i(TAG,"screen name: " + screenName);

                final String url = MessageFormat.format("https://twitter.com/#!/{0}/status/{1}",screenName,id);
                Log.i(TAG,"url: " + url);

                Runnable runnable = new Runnable() {
                    public void run() {
                        ((TextView)Main.this.findViewById(R.id.textView)).setText("Tweeted: " + url);
                    }
                };
                
                Main.this.runOnUiThread(runnable);

                return resp.getStatusLine().getStatusCode() == 200;
                
            } catch (Exception e) {
                Log.e(TAG,"trying to tweet: " + message, e);
                return false;
            }

        }
    }

    public static String convertStreamToString(java.io.InputStream is) {
        try {
            return new java.util.Scanner(is).useDelimiter("\\A").next();
        } catch (java.util.NoSuchElementException e) {
            return "";
        }
    }
    
    public static String getFirstMatch(Pattern pattern, String str){
        Matcher matcher = pattern.matcher(str);
        if(matcher.matches()){
            return matcher.group(1);
        }
        return null;
    }
}
