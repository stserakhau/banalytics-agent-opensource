import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.tesseract.TessBaseAPI;

import static org.bytedeco.opencv.global.opencv_highgui.imshow;
import static org.bytedeco.opencv.global.opencv_highgui.waitKey;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;

import org.bytedeco.javacpp.*;
import org.bytedeco.leptonica.*;
import org.bytedeco.tesseract.*;
import static org.bytedeco.leptonica.global.leptonica.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.tesseract.global.tesseract.*;

public class Test {
    public static void main1(String[] args) {
        BytePointer outText;

        TessBaseAPI api = new TessBaseAPI();
        // Initialize tesseract-ocr with English, without specifying tessdata path
        if (api.Init("e:\\tessdata\\", "osd") != 0) {
            System.err.println("Could not initialize tesseract.");
            System.exit(1);
        }

        // Open input image with leptonica library
        PIX image = pixRead(args.length > 0 ? args[0] : "e:\\alpr.png");
        api.SetPageSegMode(6);
        api.SetImage(image);
        // Get OCR result
        outText = api.GetUTF8Text();
        System.out.println("OCR output:\n" + outText.getString());

        // Destroy used object and release memory
        api.End();
        outText.deallocate();
        pixDestroy(image);
    }

    public static void main(String[] args) {//00070002
        try (TessBaseAPI api = new TessBaseAPI();
             Mat mat = imread("e:\\Tmp\\temp-output\\mat-1743172432938.jpg")
//             Mat mat = imread("e:\\text-image.png")
             ) {
            if (api.Init("e:\\tessdata\\", "eng") != 0) {
                System.err.println("Could not initialize tesseract.");
                System.exit(1);
            }
            imshow("Img", mat); waitKey(0);
            cvtColor(mat, mat, CV_BGR2GRAY);
            imshow("Img", mat); waitKey(0);
            equalizeHist(mat, mat);
            imshow("Img", mat); waitKey(0);
            adaptiveThreshold(mat, mat, 255, ADAPTIVE_THRESH_GAUSSIAN_C, THRESH_BINARY, 11, 2);
            medianBlur(mat, mat, 3);
            imshow("Img", mat); waitKey(0);
            api.SetPageSegMode(6);
            api.SetImage(mat.data(), mat.cols(), mat.rows(), 1, 1 * mat.cols());

            long st = System.currentTimeMillis();
            try (BytePointer outText = api.GetUTF8Text()) {
                long en = System.currentTimeMillis();
                System.out.println((en - st) + " ms : " + outText.getString());
            }
            api.End();
        }
    }
}
