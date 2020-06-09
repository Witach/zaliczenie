package edu.iis.mto.testreactor.dishwasher;

import static edu.iis.mto.testreactor.dishwasher.Status.DOOR_OPEN;
import static edu.iis.mto.testreactor.dishwasher.Status.SUCCESS;
import static java.util.Objects.requireNonNull;

import edu.iis.mto.testreactor.dishwasher.engine.Engine;
import edu.iis.mto.testreactor.dishwasher.engine.EngineException;
import edu.iis.mto.testreactor.dishwasher.pump.PumpException;
import edu.iis.mto.testreactor.dishwasher.pump.WaterPump;

public class DishWasher {

    public static final double MAXIMAL_FILTER_CAPACITY = 50.0d;
    private final WaterPump waterPump;
    private final Engine engine;
    private final DirtFilter dirtFilter;
    private final Door door;

    public DishWasher(WaterPump waterPump, Engine engine, DirtFilter dirtFilter, Door door) {
        this.waterPump = requireNonNull(waterPump, "waterPump = null");
        this.engine = requireNonNull(engine, "engine == null");
        this.dirtFilter = requireNonNull(dirtFilter, "dirtFilter == null");
        this.door = requireNonNull(door, "door == null");
    }

    public RunResult start(ProgramConfiguration program) {
        requireNonNull(program, "program == null");
        if (!door.closed()) {
            return error(DOOR_OPEN);
        }
        if (filterIsClean(program)) {
            return run(program.getProgram(), program.getFillLevel());
        }
        return error(Status.ERROR_FILTER);
    }

    private boolean filterIsClean(ProgramConfiguration program) {
        if (program.isWashingTabletsUsed()) {
            return dirtFilter.capacity() <= MAXIMAL_FILTER_CAPACITY;
        }
        return true;
    }

    private RunResult run(WashingProgram program, FillLevel fillLevel) {
        try {
            door.lock();
            runProgram(program, fillLevel);
        } catch (EngineException e) {
            return error(Status.ERROR_PROGRAM);
        } catch (PumpException e) {
            return error(Status.ERROR_PUMP);
        }

        return success(program);
    }

    private void runProgram(WashingProgram program, FillLevel fillLevel) throws EngineException, PumpException {
        waterPump.pour(fillLevel);
        engine.runProgram(program);
        waterPump.drain();
    }

    private RunResult success(WashingProgram program) {
        return RunResult.builder()
                .withStatus(SUCCESS)
                .withRunMinutes(program.getTimeInMinutes())
                .build();
    }

    private RunResult error(Status errorPump) {
        return RunResult.builder()
                .withStatus(errorPump)
                .build();
    }
}
