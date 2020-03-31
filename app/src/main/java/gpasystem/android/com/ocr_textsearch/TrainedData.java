package gpasystem.android.com.ocr_textsearch;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class TrainedData {

    private String path;
    private TessBaseAPI tessBaseAPI;
    Context context;
    public TrainedData(Context context) {
        this.context = context;

        tessBaseAPI = new TessBaseAPI();
    }

    public String capturedString(Bitmap bitmap, String lang) {

        tessBaseAPI.init(path, lang);
        tessBaseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO_ONLY);

        tessBaseAPI.setImage(bitmap);
        return tessBaseAPI.getUTF8Text();
    }

    public void stop() {
        if (tessBaseAPI != null)
            tessBaseAPI.end();
    }

    void checkFile(String lang)
    {
        path = Environment.getExternalStorageDirectory() + "/ocr/";
        File dir = new File(path + "/tessdata/");
        File file = new File(path + "/tessdata/" +lang+ ".traineddata");
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.e("Error"," failed to create directory");
            } else {
                copyTrainedDataToMobile(context,lang);
            }

        }
        else
        {
            if(!file.exists())
            {
                copyTrainedDataToMobile(context,lang);
            }
        }
    }


    private void copyTrainedDataToMobile(Context context,String lang) {
        AssetManager assetManager = context.getAssets();
        try {
            InputStream in = assetManager.open("tessdata/"+lang+".traineddata");
            OutputStream out = new FileOutputStream(path + "/tessdata/" + lang+".traineddata");
            byte[] buffer = new byte[1024];
            int read = in.read(buffer);
            while (read != -1) {
                out.write(buffer, 0, read);
                read = in.read(buffer);
            }
        } catch (Exception e) {
            Log.e("Error", e.toString());
        }
    }
}
