package edu.iis.mto.testreactor.dishwasher;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import edu.iis.mto.testreactor.dishwasher.engine.Engine;
import edu.iis.mto.testreactor.dishwasher.engine.EngineException;
import edu.iis.mto.testreactor.dishwasher.pump.PumpException;
import edu.iis.mto.testreactor.dishwasher.pump.WaterPump;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @BeforeEach
    public void setUp() {
        dishWasher = new DishWasher(waterPump,engine,dirtFilter,door);
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
    public void shouldReturnErrorIfDoorAreNotClosed(){
        ProgramConfiguration programConfiguration = standardProgrammeConfiguration(false);
        when(door.closed()).thenReturn(false);
        RunResult runResult = dishWasher.start(programConfiguration);
        assertEquals(Status.DOOR_OPEN, runResult.getStatus());
    }

    @Test
    public void shouldReturnErrorIfFileterIsOverloaded(){
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



    ProgramConfiguration standardProgrammeConfiguration(boolean withTabletsUsed){
        return ProgramConfiguration.builder()
                .withFillLevel(FillLevel.HALF)
                .withProgram(WashingProgram.ECO)
                .withTabletsUsed(withTabletsUsed)
                .build();
    }


}
