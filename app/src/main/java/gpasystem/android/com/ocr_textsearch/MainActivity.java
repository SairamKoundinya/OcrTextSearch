package gpasystem.android.com.ocr_textsearch;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private TextView capturedString;
    private EditText searchtext;
    private TrainedData trainedData;
    private static final int READ_REQUEST_CODE = 42;
    private String language, result;
    private boolean track;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        capturedString = findViewById(R.id.capturedString);
        searchtext = findViewById(R.id.searchtext);
        searchtext.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        imageView = findViewById(R.id.image);

        language = "eng";
        trainedData = new TrainedData(MainActivity.this);
        if(isWSPGranted())
        trainedData.checkFile(language);

        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        spinner.setOnItemSelectedListener(this);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.language, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(adapter);
    }

    public void imagescan(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");

        track = true;
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    public void pdfscan(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");

        track = false;
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {

            if (resultData != null) {

                if(track)
                    setImage(resultData);
                else
                    pdfToBitmap(resultData);
            }
        }
    }
    private void setImage(Intent resultData) {
       Uri uri = resultData.getData();
         ParcelFileDescriptor parcelFileDescriptor =null;

        try {
            if (uri != null) {
                parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
            }
            FileDescriptor fileDescriptor = null;
            if (parcelFileDescriptor != null) {
                fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                parcelFileDescriptor.close();
                if(isWSPGranted() ) {
                    result = trainedData.capturedString(image, language);
                    imageView.setImageBitmap(image);
                    capturedString.setText(result);
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private  void pdfToBitmap(Intent resultData) {

        Uri uri = resultData.getData();
        Bitmap bitmap;
        try {
            PdfRenderer renderer = new PdfRenderer(getContentResolver().openFileDescriptor(uri, "r"));
            PdfRenderer.Page page = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                page = renderer.openPage(0);
            }
            //int width = getResources().getDisplayMetrics().densityDpi / 72 * page.getWidth();
           // int height = getResources().getDisplayMetrics().densityDpi / 72 * page.getHeight();
            bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

            page.close();
            if( isWSPGranted()) {
                result = trainedData.capturedString(bitmap, language);
                imageView.setImageBitmap(bitmap);
                capturedString.setText(result);
            }
            renderer.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void search(View view) {

        String input = searchtext.getText().toString();

        SpannableString spannableString = new SpannableString(capturedString.getText().toString());

        String[] words = input.split(" ");

        for(String word : words)
        {
           spanned(word, spannableString, true);
        }

        spanned(input, spannableString, false);
        capturedString.setText(spannableString);

    }

    public  void spanned(String word, SpannableString spannableString, boolean foreground)
    {
        int startindex = 0;

        while (true) {

            int index = result.indexOf(word, startindex);

            if(index != -1)
            {
                if(foreground) {
                    ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(Color.RED);
                    spannableString.setSpan(foregroundColorSpan, index, index + word.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                else
                {
                    BackgroundColorSpan backgroundColorSpan = new BackgroundColorSpan(Color.YELLOW);
                    spannableString.setSpan(backgroundColorSpan, index, index + word.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            else break;
            startindex = index+word.length();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

           if(isWSPGranted() ) {
               switch (position) {
                   case 0:
                       language = "eng";
                       trainedData.checkFile(language);
                       break;
                   case 1:
                       language = "tel";
                       trainedData.checkFile(language);
                       break;
                   case 2:
                       language = "urd";
                       trainedData.checkFile(language);
                       break;
                   default:
                       language = "eng";
                       break;
               }
           }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }


    public  boolean isWSPGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
               // Log.v(TAG,"Permission is granted2");
                return true;
            } else {
                     ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
                return false;
            }
        }
        else  return true;

    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 2) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                trainedData.checkFile(language);
            }
        }
    }
}
