package edu.iis.mto.testreactor.dishwasher;

import edu.iis.mto.testreactor.dishwasher.engine.Engine;
import edu.iis.mto.testreactor.dishwasher.engine.EngineException;
import edu.iis.mto.testreactor.dishwasher.pump.PumpException;
import edu.iis.mto.testreactor.dishwasher.pump.WaterPump;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DishWasherTest {

    DishWasher dishWasher;
    @Mock
    WaterPump waterPump;
    @Mock
    Door door;
    @Mock
    DirtFilter dirtFilter;
    @Mock
    Engine engine;

    @Captor
    ArgumentCaptor<FillLevel> captor;

    @Captor
    ArgumentCaptor<WashingProgram> programArgumentCaptor;

    @BeforeEach
    public void setUp() {
        dishWasher = new DishWasher(waterPump, engine, dirtFilter, door);
    }

    @Test
    public void itCompiles() {
        assertThat(true, Matchers.equalTo(true));
    }

    @Test
    public void shouldStartProgramme() {
        ProgramConfiguration programConfiguration = standardProgrammeConfiguration(false);
        when(door.closed()).thenReturn(true);
        RunResult runResult = dishWasher.start(programConfiguration);
        assertEquals(Status.SUCCESS, runResult.getStatus());
    }

    @Test
    public void shouldReturnErrorIfDoorAreNotClosed() {
        ProgramConfiguration programConfiguration = standardProgrammeConfiguration(false);
        when(door.closed()).thenReturn(false);
        RunResult runResult = dishWasher.start(programConfiguration);
        assertEquals(Status.DOOR_OPEN, runResult.getStatus());
    }

    @Test
    public void shouldReturnErrorIfFileterIsOverloaded() {
        ProgramConfiguration programConfiguration = standardProgrammeConfiguration(true);
        when(door.closed()).thenReturn(true);
        when(dirtFilter.capacity()).thenReturn(51.d);
        RunResult runResult = dishWasher.start(programConfiguration);
        assertEquals(Status.ERROR_FILTER, runResult.getStatus());
    }

    @Test
    public void shouldFailWhenEngineIsFaulty() throws EngineException {
        ProgramConfiguration programConfiguration = standardProgrammeConfiguration(true);
        when(door.closed()).thenReturn(true);
        when(dirtFilter.capacity()).thenReturn(29.d);
        doThrow(new EngineException()).when(engine).runProgram(any());
        RunResult runResult = dishWasher.start(programConfiguration);
        assertEquals(Status.ERROR_PROGRAM, runResult.getStatus());
    }

    @Test
    public void shouldFailWhenPumpIsFaulty() throws PumpException {
        ProgramConfiguration programConfiguration = standardProgrammeConfiguration(true);
        when(door.closed()).thenReturn(true);
        when(dirtFilter.capacity()).thenReturn(29.d);
        doThrow(new PumpException()).when(waterPump).pour(any());
        RunResult runResult = dishWasher.start(programConfiguration);
        assertEquals(Status.ERROR_PUMP, runResult.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = WashingProgram.class, names = {"INTENSIVE", "ECO", "RINSE", "NIGHT"})
    public void shouldRunForExactPeriodOfTime(WashingProgram type) {
        ProgramConfiguration programConfiguration = ProgramConfiguration.builder()
                .withFillLevel(FillLevel.HALF)
                .withProgram(type)
                .withTabletsUsed(true)
                .build();

        when(door.closed()).thenReturn(true);
        when(dirtFilter.capacity()).thenReturn(29.d);
        RunResult runResult = dishWasher.start(programConfiguration);

        assertEquals(type.getTimeInMinutes(), runResult.getRunMinutes());
    }

    @Test
    public void shouldUserPartsInExactSequence() throws EngineException, PumpException {
        ProgramConfiguration programConfiguration = standardProgrammeConfiguration(true);

        when(door.closed()).thenReturn(true);
        when(dirtFilter.capacity()).thenReturn(29.d);
        doNothing().when(door).lock();
        doNothing().when(engine).runProgram(any());
        doNothing().when(waterPump).drain();
        doNothing().when(waterPump).pour(any());

        InOrder inOrder = inOrder(door, dirtFilter, door, waterPump,  engine ,waterPump);


        RunResult runResult = dishWasher.start(programConfiguration);

        verify(door, times(1)).closed();
        verify(dirtFilter, times(1)).capacity();
        verify(door, times(1)).lock();
        verify(engine, times(1)).runProgram(any());
        verify(waterPump, times(1)).drain();

        then(door).should(inOrder).closed();
        then(dirtFilter).should(inOrder).capacity();
        then(door).should(inOrder).lock();
        then(waterPump).should(inOrder).pour(any());
        then(engine).should(inOrder).runProgram(any());
        then(waterPump).should(inOrder).drain();

    }

    @Test
    public void shouldNotUseDirtFilterIfNoTabletsUsed(){
        ProgramConfiguration programConfiguration = standardProgrammeConfiguration(false);

        when(door.closed()).thenReturn(true);


        RunResult runResult = dishWasher.start(programConfiguration);

        verify(dirtFilter, never()).capacity();
    }

    @Test
    public void shouldUseProvidedFillLevel() throws PumpException {
        ProgramConfiguration programConfiguration = standardProgrammeConfiguration(false);

        when(door.closed()).thenReturn(true);


        RunResult runResult = dishWasher.start(programConfiguration);
        verify(waterPump).pour(captor.capture());
        assertEquals(FillLevel.HALF, captor.getValue());
    }

    @Test
    public void shouldUseProvidedProgram() throws PumpException, EngineException {
        ProgramConfiguration programConfiguration = standardProgrammeConfiguration(false);

        when(door.closed()).thenReturn(true);


        RunResult runResult = dishWasher.start(programConfiguration);
        verify(engine).runProgram(programArgumentCaptor.capture());
        assertEquals(programArgumentCaptor.getValue(), programArgumentCaptor.getValue());
    }

    @Test
    public void shouldNotUseEngineAfterWaterPumpFailing() throws EngineException, PumpException {
        ProgramConfiguration programConfiguration = standardProgrammeConfiguration(false);

        when(door.closed()).thenReturn(true);
        doThrow(new PumpException()).when(waterPump).pour(any());



        RunResult runResult = dishWasher.start(programConfiguration);

        verify(engine, never()).runProgram(any());
    }

    @Test
    public void shouldNotDrainAfterEngineFailure() throws EngineException, PumpException {
        ProgramConfiguration programConfiguration = standardProgrammeConfiguration(false);

        when(door.closed()).thenReturn(true);

        doThrow(new EngineException()).when(engine).runProgram(any());


        RunResult runResult = dishWasher.start(programConfiguration);

        verify(waterPump, never()).drain();
    }


    ProgramConfiguration standardProgrammeConfiguration(boolean withTabletsUsed) {
        return ProgramConfiguration.builder()
                .withFillLevel(FillLevel.HALF)
                .withProgram(WashingProgram.ECO)
                .withTabletsUsed(withTabletsUsed)
                .build();
    }


}
