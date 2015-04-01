package llvmast;
public class LlvmLabel extends LlvmInstruction{
    public LlvmLabelValue label;
    public LlvmLabel(LlvmLabelValue label){this.label = label;}
    public String toString(){
	return label+":";
    }
}