package in.co.ophio.secure.core;

import android.app.Application;
import android.security.KeyChain;
import android.util.Base64;

import com.example.android.vault.SecretKeyWrapper;
import com.example.android.vault.Utils;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author Gaurav Vashisth (vashisthg@gmail.com)
 */
public class KeyGenerator {

    public static final String TAG = "SecretKeyUtils";
    private static final int DATA_KEY_LENGTH = 32;

    private Application application;
    /**
     * Flag indicating that the {@link SecretKeyWrapper} public/private key is
     * hardware-backed. A software keystore is more vulnerable to offline
     * attacks if the device is compromised.
     */
    private boolean isHardwareBacked;
    /** File where wrapped symmetric key is stored. */
    private File keyFile;

    public static KeyGenerator get(Application application, String fileName) {
        return new KeyGenerator(application, fileName);
    }

    public KeyGenerator(Application application, String filename) {
        this.application = application;
        this.isHardwareBacked = KeyChain.isBoundKeyAlgorithm("RSA");
        this.keyFile = new File(application.getFilesDir(), filename);

        try {
            // Load secret key and ensure our root document is ready.
            loadOrGenerateKeys();

        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public String loadOrGenerateKeys()
            throws GeneralSecurityException, IOException {
        final SecretKeyWrapper wrapper = new SecretKeyWrapper(application, TAG);
        // Generate secret key if none exists
        if (!keyFile.exists()) {
            final byte[] raw = new byte[DATA_KEY_LENGTH];
            new SecureRandom().nextBytes(raw);
            final SecretKey key = new SecretKeySpec(raw, "AES");
            final byte[] wrapped = wrapper.wrap(key);
            Utils.writeFully(keyFile, wrapped);
        }
        // Even if we just generated the key, always read it back to ensure we
        // can read it successfully.
        final byte[] wrapped = Utils.readFully(keyFile);
        final SecretKey key = wrapper.unwrap(wrapped);

        return Base64.encodeToString(key.getEncoded(), Base64
                .DEFAULT);
    }

    public boolean isHardwareBacked() {
        return isHardwareBacked;
    }
}
