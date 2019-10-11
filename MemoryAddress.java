package sample;

public class MemoryAddress {
    private String dataValue;
    private String addressValue;

    public MemoryAddress(String dataValue, String addressValue) {
        this.addressValue = addressValue;
        this.dataValue = dataValue;
    }

    public String getAddressValue() {
        return addressValue;
    }

    public String getDataValue() {
        return dataValue;
    }

    public void setAddressValue(String addressValue) {
        this.addressValue = addressValue;
    }

    public void setDataValue(String dataValue) {
        this.dataValue = dataValue;
    }

    @Override
    public int hashCode() {
        return addressValue.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this.getClass() != obj.getClass()){
            return false;
        }
        MemoryAddress a = (MemoryAddress) obj;
        return this.addressValue.equals(a.addressValue);
    }
}
