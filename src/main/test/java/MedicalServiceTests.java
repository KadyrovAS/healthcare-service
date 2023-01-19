import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import net.bytebuddy.asm.Advice;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.netology.patient.entity.BloodPressure;
import ru.netology.patient.entity.HealthInfo;
import ru.netology.patient.entity.PatientInfo;
import ru.netology.patient.repository.PatientInfoFileRepository;
import ru.netology.patient.repository.PatientInfoRepository;
import ru.netology.patient.service.alert.SendAlertService;
import ru.netology.patient.service.alert.SendAlertServiceImpl;
import ru.netology.patient.service.medical.MedicalService;
import ru.netology.patient.service.medical.MedicalServiceImpl;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MedicalServiceTests {
    static PatientInfo patient;
    static PatientInfoRepository patientInfoRepository;
    static String id;

    @BeforeAll
    public static void initialValues(){
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModules(new JavaTimeModule(), new ParameterNamesModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        File repoFile = new File("patients.txt");
        if (repoFile.exists())
            repoFile.delete();
        patientInfoRepository = new PatientInfoFileRepository(repoFile, mapper);

        patient = new PatientInfo("Иван", "Петров", LocalDate.of(1980, 11, 26),
                new HealthInfo(new BigDecimal("36.65"), new BloodPressure(120, 80)));

        id = patientInfoRepository.add(patient);
    }

    @Test
    public void bloodPressureTest(){
        SendAlertService alertServiceMock = Mockito.mock(SendAlertServiceImpl.class);

        MedicalService medicalService = new MedicalServiceImpl(patientInfoRepository, alertServiceMock);

        BloodPressure bloodPressure = new BloodPressure(
                patient.getHealthInfo().getBloodPressure().getHigh() + 1,
                     patient.getHealthInfo().getBloodPressure().getLow() - 1
        );

        ArgumentCaptor<String>argumentCaptor = ArgumentCaptor.forClass(String.class);
        medicalService.checkBloodPressure(id, bloodPressure);

        Mockito.verify(alertServiceMock).send(argumentCaptor.capture());

        String expected = String.format("Warning, patient with id: %s, need help", id);
        String actual = argumentCaptor.getValue();

        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void temperatureTest(){
        SendAlertService alertServiceMock = Mockito.mock(SendAlertServiceImpl.class);

        MedicalService medicalService = new MedicalServiceImpl(patientInfoRepository, alertServiceMock);

        BigDecimal currentTemperature = patient.getHealthInfo().getNormalTemperature()
                .subtract(BigDecimal.valueOf(2));

        medicalService.checkTemperature(id, currentTemperature);

        ArgumentCaptor<String>argumentCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(alertServiceMock).send(argumentCaptor.capture());

        String expected = String.format("Warning, patient with id: %s, need help", id);
        String actual = argumentCaptor.getValue();

        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void bloodPressureTestNormal(){
        SendAlertService alertServiceMock = Mockito.mock(SendAlertServiceImpl.class);

        MedicalService medicalService = new MedicalServiceImpl(patientInfoRepository, alertServiceMock);

        BloodPressure bloodPressure = patient.getHealthInfo().getBloodPressure();

        medicalService.checkBloodPressure(id, bloodPressure);

        Mockito.verify(alertServiceMock, Mockito.never()).send(Mockito.anyString());
    }

    @Test
    public void temperatureTestNormal(){
        SendAlertService alertServiceMock = Mockito.mock(SendAlertServiceImpl.class);

        MedicalService medicalService = new MedicalServiceImpl(patientInfoRepository, alertServiceMock);

        BigDecimal currentTemperature = patient.getHealthInfo().getNormalTemperature();

        medicalService.checkTemperature(id, currentTemperature);

        Mockito.verify(alertServiceMock, Mockito.never()).send(Mockito.anyString());
    }

}
