package ufrgs.encryption_sisemb;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.security.GeneralSecurityException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private static final int MB = 1024*1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Raw string of X*MB. Change multiplier to manipulate input size.
        char[] raw = new char[11*MB];
        Arrays.fill(raw, 'x');

        String password = "password";
        String message = new String(raw);

        // AES Encryption
        try {
            String encryptedMsg = AESCrypt.encrypt(password, message);
            Log.d("ENCRYPTED-AES: ", encryptedMsg);
        }catch (GeneralSecurityException e){
            e.printStackTrace();
        }

        // Twofish Encryption
        try {
            String encryptedMsg = TwofishCrypt.encrypt(password, message);
            Log.d("ENCRYPTED-TWOFISH: ", encryptedMsg);
        }catch (GeneralSecurityException e){
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}
