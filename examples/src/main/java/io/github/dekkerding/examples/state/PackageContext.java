package io.github.dekkerding.examples.state;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class PackageContext {
    private PackageState currentState;
    private String packageld;

    public PackageContext(PackageState currentState, String packageld) {
        this.currentState = currentState;
        this.packageld = packageld;
        if (currentState == null){
            this.currentState = Acknowledged.getInstance();
        }
    }

    public void update(){
        currentState.updateState(this);
    }
}