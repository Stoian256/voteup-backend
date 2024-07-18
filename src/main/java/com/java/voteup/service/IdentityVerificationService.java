package com.java.voteup.service;

import com.java.voteup.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.leptonica.PIX;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.tesseract.TessBaseAPI;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.bytedeco.leptonica.global.lept.pixRead;
import static org.opencv.imgcodecs.Imgcodecs.imread;

@Service
@RequiredArgsConstructor
public class IdentityVerificationService {
    private final UserRepository userRepository;
    public ResponseEntity<String> extractTextFromImage(File imageFile, String userId) {
        Loader.load(org.bytedeco.tesseract.global.tesseract.class);

        TessBaseAPI api = new TessBaseAPI();
        api.Init("tessdata", "ron");



        PIX image = pixRead(imageFile.getAbsolutePath());
        /*Mat image1 = imread(imageFile.getAbsolutePath());
        Mat grayImage = new Mat();
        Imgproc.cvtColor(image1, grayImage, Imgproc.COLOR_BGR2GRAY);
        Mat adjustedImage = new Mat();
        float alpha = 1.5F; // Factorul de ajustare a contrastului
        float beta = 50;  // Factorul de ajustare a luminozității
        grayImage.convertTo(adjustedImage, -1, alpha, beta);
        Mat denoisedImage = new Mat();
        Imgproc.GaussianBlur(adjustedImage, denoisedImage, new Size(3, 3), 0);
        String outputImagePath = "C:\\Users\\silvi\\Desktop\\SEMESTUL 6\\ELL\\aplicatie\\voteup\\data\\imagine_preprocesata.jpg";
        Imgcodecs.imwrite(outputImagePath, denoisedImage);*/
        //tranform();

        api.SetImage(image);

        BytePointer resultPointer = api.GetUTF8Text();
        String result = resultPointer.getString();


        Pattern cnpPattern = Pattern.compile("CNP (\\d{13})");
        Matcher cnpMatcher = cnpPattern.matcher(result);
        Pattern dataPattern = Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{2})-(\\d{2}\\.\\d{2}\\.\\d{4})");
        Matcher dataMatcher = dataPattern.matcher(result);
        api.End();
        imageFile.delete();
        if (cnpMatcher.find() && dataMatcher.find()) {
            String cnp = cnpMatcher.group(1);
            LocalDate birthDate =extractBirthDate(cnp);
            String dataExpirare = dataMatcher.group(2);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

            if (LocalDate.parse(dataExpirare,formatter).isBefore(LocalDate.now()))
                return ResponseEntity.ok("Documentul nu este valid: cartea de indentitate este expirata");
            if(!cnp.equals(userRepository.findByUserIdEquals(Integer.parseInt(userId)).getCnp()))
                return ResponseEntity.ok("Datele introduse la inregistrare nu sunt confirmate:  cnp invalide");
            if(Period.between(birthDate,LocalDate.now()).getYears() <18)
                return ResponseEntity.ok("Nu aveti varsta necesara pentru a vota");

            userRepository.updateValidatedByUserIdEquals(true,Integer.parseInt(userId));
            return ResponseEntity.ok("Identitate validata!");


        } else {
            return ResponseEntity.ok("Nu am putut identifica automat datele. Incarcati alta imagine sau contactati un operator.");
        }
    }
    private LocalDate extractBirthDate(String cnp){


        // Extrage anul, luna și ziua din CNP
        int year = Integer.parseInt(cnp.substring(1, 3));
        int month = Integer.parseInt(cnp.substring(3, 5));
        int day = Integer.parseInt(cnp.substring(5, 7));

        // Determină secolul în funcție de prima cifră din CNP
        int century = 0;
        int firstDigit = Character.getNumericValue(cnp.charAt(0));
        if (firstDigit == 1 || firstDigit == 2) {
            century = 19;
        } else if (firstDigit == 3 || firstDigit == 4) {
            century = 20;
        } else if (firstDigit == 5 || firstDigit == 6) {
            century = 21;
        }

        // Creează obiectul LocalDate pentru data de naștere
        LocalDate birthDate = LocalDate.of((century-1) * 100 + year, month, day);

        return birthDate;
    }

}
