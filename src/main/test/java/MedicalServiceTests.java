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
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MedicalServiceTests {
    static List<PatientInfo> patients;
    static PatientInfoRepository patientInfoRepository;

    @BeforeAll
    public static void initialValues(){
        patients = new ArrayList<>();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModules(new JavaTimeModule(), new ParameterNamesModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        File repoFile = new File("patients.txt");
        patientInfoRepository = new PatientInfoFileRepository(repoFile, mapper);

        try(Scanner scanner = new Scanner(repoFile)){
            while (scanner.hasNextLine()){
                PatientInfo patientInfo = mapper.readValue(scanner.nextLine(), PatientInfo.class);
                patients.add(patientInfo);
            }
        }catch (IOException e){
            e.printStackTrace();
        }

    }

    @Test
    public void bloodPressureTest(){
        int n = 0;
        SendAlertService alertServiceMock = Mockito.mock(SendAlertServiceImpl.class);

        MedicalService medicalService = new MedicalServiceImpl(patientInfoRepository, alertServiceMock);

        String id1 = patients.get(n).getId();
        BloodPressure bloodPressure = new BloodPressure(
                patients.get(n).getHealthInfo().getBloodPressure().getHigh() + 1,
                     patients.get(n).getHealthInfo().getBloodPressure().getLow() - 1
        );

        ArgumentCaptor<String>argumentCaptor = ArgumentCaptor.forClass(String.class);
        medicalService.checkBloodPressure(id1, bloodPressure);

        Mockito.verify(alertServiceMock).send(argumentCaptor.capture());

        String expected = String.format("Warning, patient with id: %s, need help", id1);
        String actual = argumentCaptor.getValue();

        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void temperatureTest(){
        SendAlertService alertServiceMock = Mockito.mock(SendAlertServiceImpl.class);

        MedicalService medicalService = new MedicalServiceImpl(patientInfoRepository, alertServiceMock);

        String id1 = patients.get(0).getId();
        BigDecimal currentTemperature = patients.get(0).getHealthInfo().getNormalTemperature()
                .subtract(BigDecimal.valueOf(2));

        medicalService.checkTemperature(id1, currentTemperature);

        ArgumentCaptor<String>argumentCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(alertServiceMock).send(argumentCaptor.capture());

        String expected = String.format("Warning, patient with id: %s, need help", id1);
        String actual = argumentCaptor.getValue();

        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void bloodPressureTestNormal(){
        int n = 0;
        SendAlertService alertServiceMock = Mockito.mock(SendAlertServiceImpl.class);

        MedicalService medicalService = new MedicalServiceImpl(patientInfoRepository, alertServiceMock);

        String id1 = patients.get(n).getId();
        BloodPressure bloodPressure = patients.get(n).getHealthInfo().getBloodPressure();

        medicalService.checkBloodPressure(id1, bloodPressure);

        Mockito.verify(alertServiceMock, Mockito.never()).send(Mockito.anyString());
    }

    @Test
    public void temperatureTestNormal(){
        SendAlertService alertServiceMock = Mockito.mock(SendAlertServiceImpl.class);

        MedicalService medicalService = new MedicalServiceImpl(patientInfoRepository, alertServiceMock);

        String id1 = patients.get(0).getId();
        BigDecimal currentTemperature = patients.get(0).getHealthInfo().getNormalTemperature();

        medicalService.checkTemperature(id1, currentTemperature);

        Mockito.verify(alertServiceMock, Mockito.never()).send(Mockito.anyString());
    }

}
