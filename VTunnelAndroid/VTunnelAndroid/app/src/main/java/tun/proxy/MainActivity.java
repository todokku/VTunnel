package tun.proxy;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.VpnService;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import androidx.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.thefinestartist.finestwebview.FinestWebView;

import tun.proxy.service.Tun2HttpVpnService;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.io.IOException;
import java.net.URI;


public class MainActivity extends AppCompatActivity {
    public static final int REQUEST_VPN = 1;
    public static final int REQUEST_CERT = 2;

    Button start;
    Button stop;
    EditText hostEditText;
    EditText searchBox;
    Button searchButton;
    Button imageSearchButton;
    MenuItem menuSetting;
    Handler statusHandler = new Handler();

    private Tun2HttpVpnService service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        start = findViewById(R.id.start);
        stop = findViewById(R.id.stop);
        hostEditText = findViewById(R.id.host);
        searchBox = findViewById(R.id.search_src_text);
        searchButton = findViewById(R.id.search_go_btn);
        imageSearchButton = findViewById(R.id.search_voice_btn);

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AsyncTaskRunner runner = new AsyncTaskRunner();
                String sleepTime = "15";
                runner.execute(sleepTime);
//                MyApplication app = (MyApplication) getApplicationContext();
//                byte [] trust_ca = app.getTrustCA();
//                if (trust_ca != null) {
//                    Intent intent = CertificateUtil.trustRootCA(CertificateUtil.getCACertificate(trust_ca));
//                    if (intent != null) {
//                        startActivityForResult(intent, REQUEST_CERT);
//                    } else {
                //startVpn();
//                    }
//                }
            }
        });
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String appFileDirectory = getFilesDir().getPath();
                try {
                killProcess(new File(appFileDirectory + "/pid"));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                String path = appFileDirectory + "/config.conf";
                File file = new File(path);
                if (file.exists()){
                    file.delete();
                }

                stopVpn();
            }
        });
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchIt();
            }
        });
        imageSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageSearch();
            }
        });

        start.setEnabled(true);
        stop.setEnabled(false);
        searchButton.setEnabled(false);
        imageSearchButton.setEnabled(false);

        loadHostPort();
//        requestPermission();
    }
    private void killProcess(File pidFile) throws IOException {
        File f = pidFile;
        FileReader inputF = new FileReader(f);
        BufferedReader in = new BufferedReader(inputF);
        String s =in.readLine();
        android.os.Process.killProcess(Integer.parseInt(s));
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_activity_settings);
        item.setEnabled(start.isEnabled());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_activity_settings:
                Intent intent = new android.content.Intent(this, SimplePreferenceActivity.class);
                startActivity(intent);
                break;
            case R.id.action_show_about:
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.app_name) + " v" + getVersionName() + ", Fork of TunProxy")
                        .setMessage("Disclaimer: May not work on some Android10.")
                        .show();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    String getVersionName() {
        PackageManager packageManager = getPackageManager();
        if (packageManager == null) {
            return null;
        }

        try {
            return packageManager.getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            Tun2HttpVpnService.ServiceBinder serviceBinder = (Tun2HttpVpnService.ServiceBinder) binder;
            service = serviceBinder.getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            service = null;
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        start.setEnabled(false);
        stop.setEnabled(false);
        searchButton.setEnabled(false);
        imageSearchButton.setEnabled(false);
        updateStatus();

        statusHandler.post(statusRunnable);

        Intent intent = new Intent(this, Tun2HttpVpnService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    boolean isRunning() {
        return service != null && service.isRunning();
    }

    Runnable statusRunnable = new Runnable() {
        @Override
        public void run() {
        updateStatus();
        statusHandler.post(statusRunnable);
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        statusHandler.removeCallbacks(statusRunnable);
        unbindService(serviceConnection);
    }

    void updateStatus() {
        if (service == null) {
            return;
        }
        if (isRunning()) {
            start.setEnabled(false);
            hostEditText.setEnabled(false);
            stop.setEnabled(true);
            searchButton.setEnabled(true);
            imageSearchButton.setEnabled(true);
        } else {
            start.setEnabled(true);
            hostEditText.setEnabled(true);
            stop.setEnabled(false);
            searchButton.setEnabled(false);
            imageSearchButton.setEnabled(false);
        }
    }

    private void stopVpn() {
        start.setEnabled(true);
        stop.setEnabled(false);
        searchButton.setEnabled(false);
        imageSearchButton.setEnabled(false);
        Tun2HttpVpnService.stop(this);
    }

    private void startVpn() {
        Intent i = VpnService.prepare(this);
        if (i != null) {
            startActivityForResult(i, REQUEST_VPN);
        } else {
            onActivityResult(REQUEST_VPN, RESULT_OK, null);
        }
    }

    private boolean searchIt() {
        String searchItem = searchBox.getText().toString();
        if (searchItem.isEmpty()) {
            searchBox.setError("Enter valid search keyword.");
            return false;
        }
        String searchItemInUrl = searchItem.replaceAll(" ", "%20");
        URI url = URI.create("https://api.serpprovider.com/search?api_key=" + "API_KEY" + "&q=" + searchItemInUrl + "&device=mobile&output=html");
        //searchBox.setText(url.toString());
        new FinestWebView.Builder(this)
                .titleDefault("Google Search")
                .theme(R.style.FinestWebViewTheme)
                .showUrl(false)
                .showIconMenu(true)
                .updateTitleFromHtml(false)
                //.iconDefaultColorRes(R.color.finestWhite)
                .show(url.toString());
        return true;
    }

    private boolean imageSearch() {
        String searchItem = searchBox.getText().toString();
        if (searchItem.isEmpty()) {
            searchBox.setError("Enter valid search keyword.");
            return false;
        }
        String searchItemInUrl = searchItem.replaceAll(" ", "%20");
        URI url = URI.create("https://api.serpprovider.com/search?api_key=" + "API_KEY" + "&q=" + searchItemInUrl + "&device=desktop&output=html&search_type=images");
        //searchBox.setText(url.toString());
        new FinestWebView.Builder(this)
                .titleDefault("Google Image Search")
                .theme(R.style.FinestWebViewTheme_Light)
                .showUrl(false)
                .showIconMenu(true)
                .updateTitleFromHtml(false)
                //.iconDefaultColorRes(R.color.finestGray)
                .show(url.toString());
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        if (requestCode == REQUEST_VPN && parseAndSaveHostPort()) {
            start.setEnabled(false);
            stop.setEnabled(true);
            searchButton.setEnabled(true);
            imageSearchButton.setEnabled(true);
            Tun2HttpVpnService.start(this);
        }
    }

    private void loadHostPort() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String proxyHost = prefs.getString(Tun2HttpVpnService.PREF_PROXY_HOST, "");
        int proxyPort = prefs.getInt(Tun2HttpVpnService.PREF_PROXY_PORT, 0);
        //String proxyHost = "127.0.0.1";
        //int proxyPort = 1080;

        if (TextUtils.isEmpty(proxyHost)) {
            return;
        }
        hostEditText.setText(proxyHost + ":" + proxyPort);
    }

    private boolean parseAndSaveHostPort() {
        String hostPort = hostEditText.getText().toString();
        if (hostPort.isEmpty()) {
            hostEditText.setError(getString(R.string.enter_host));
            return false;
        }

        String parts[] = hostPort.split(":");
        int port = 0;
        if (parts.length > 1) {
            try {
                port = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                hostEditText.setError(getString(R.string.enter_host));
                return false;
            }
            if (!(0 < port && port < 65536)) {
                hostEditText.setError(getString(R.string.enter_host));
                return false;
            }
        }
        String[] ipParts = parts[0].split("\\.");
        if (ipParts.length != 4) {
            hostEditText.setError(getString(R.string.enter_host));
            return false;
        }
        String host = parts[0];
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor edit = prefs.edit();

        edit.putString(Tun2HttpVpnService.PREF_PROXY_HOST, host);
        edit.putInt(Tun2HttpVpnService.PREF_PROXY_PORT, port);

        edit.commit();
        return true;
    }

//    private void requestPermission() {
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {//Can add more as per requirement
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 8000);
//        }
//    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
//        switch (requestCode) {
//            case 8000: {
//                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                } else {
//                    requestPermission();
//                }
//                return;
//            }
//        }
//    }
//
    private class AsyncTaskRunner extends AsyncTask<String, String, String> {

    private String resp;
    ProgressDialog progressDialog;
    private URI server;
    private String path;
    private String outputString;
    private String interfaceAddress;

    @Override
    protected String doInBackground(String... params) {
        publishProgress("Sleeping..."); // Calls onProgressUpdate()
        try {
            int time = Integer.parseInt(params[0])*1000;

            Thread.sleep(time);

            resp = "Slept for " + params[0] + " seconds";

            copyAssets("stunnel");
            copyAssets("cert");
            File file = getFileStreamPath ("stunnel");
            file.setExecutable(true);

            final int MY_PERMISSIONS_REQUEST_INTERNET=1;
            if (hasInternetPermission(MY_PERMISSIONS_REQUEST_INTERNET)) {
            }
            URI url = URI.create("https://github.com" + "/apps/" + "yourApp2");

            Document doc = Jsoup
                    .connect(url.toString())
                    .userAgent("Mozilla/5.0 (Linux; Android 8.0.0;) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.149 Mobile Safari/537.36")
                    .get();
            String title = doc.title();
            Elements links = doc.select("a[href]");

            for (Element link : links) {

                if (link.attr("abs:href").contains("amazonaws.com")) {

                    URI server = URI.create(link.attr("abs:href"));

                    String appFileDirectory = getFilesDir().getPath();
                    //path = server.getHost();

                    path = appFileDirectory + "/config.conf";
                    File file1 = new File(path);
                    if (file1.exists()){
                        file1.delete();
                    }
                    usingBufferedWritter("foreground = yes", path);
                    usingBufferedWritter("client = yes", path);
                    usingBufferedWritter("pid = " + appFileDirectory + "/pid", path);

                    usingBufferedWritter("[tinyproxy]", path);
                    usingBufferedWritter("accept = 127.0.0.1:51043", path);
                    usingBufferedWritter( "connect = " + server.getHost() + ":3128", path);

                    usingBufferedWritter("[minisocks]", path);
                    usingBufferedWritter("accept = 127.0.0.1:53087", path);
                    usingBufferedWritter( "connect = " + server.getHost() + ":1082", path);


                    ProcessBuilder builder = new ProcessBuilder(appFileDirectory + "/stunnel", "config.conf");
                    builder.directory(new File(getFilesDir().getPath()));
                    Process p = builder.start(); // may throw IOException

                }
            }


            resp = "Slept for " + "some" + " seconds";

        } catch (InterruptedException e) {
            e.printStackTrace();
            resp = e.getMessage();
        } catch (Exception e) {
            e.printStackTrace();
            resp = e.getMessage();
        }

        return resp;
    }

    private void usingBufferedWritter(String input, String path) throws IOException
    {
        String fileContent = input;

        BufferedWriter writer = new BufferedWriter(new FileWriter(path,true));
        writer.append(fileContent);
        writer.newLine();
        writer.close();
    }

    //https://stackoverflow.com/questions/5583487/hosting-an-executable-within-android-application
    private void copyAssets(String filename) {

        AssetManager assetManager = getAssets();

        InputStream in = null;
        OutputStream out = null;

        try {
            in = assetManager.open(filename);
            String appFileDirectory = getFilesDir().getPath();

            File outFile = new File(appFileDirectory, filename);
            out = new FileOutputStream(outFile);
            copyFileUsingStream(in, out);
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
        } catch(IOException e) {
            e.printStackTrace();
        }

    }

    private void copyFileUsingStream(InputStream source, OutputStream dest) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = source;
            os = dest;
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            is.close();
            os.close();
        }
    }

    private boolean hasInternetPermission(int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.INTERNET}, requestCode);
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }


    @Override
    protected void onPostExecute(String result) {
        // execution of result of Long time consuming operation
        progressDialog.dismiss();
        startVpn();
        //finalResult.setText(result);
    }

    @Override
    protected void onPreExecute() {
        progressDialog = ProgressDialog.show(MainActivity.this,
                "Connecting to the server...",
                "Please wait for a while.");

    }

    @Override
    protected void onProgressUpdate(String... text) {
        //finalResult.setText(text[0]);
        // Things to be done while execution of long running operation is in
        // progress. For example updating ProgessDialog
    }
    }
}
