package in.letsecho.echoapp.library;

import java.util.ArrayList;
import java.util.Map;

public class FbEducation {
    private String school;
    private String type;
    private String year;
    private String concentration;

    public FbEducation() {}

    public FbEducation(Map eduObj) {
        Map schoolObj = (Map)eduObj.get("school");
        if(schoolObj != null)
            school = (String)schoolObj.get("name");

        Map yearObj = (Map)eduObj.get("year");
        if(yearObj != null)
            year = (String)yearObj.get("name");

        ArrayList<Map> concentrationObj = (ArrayList<Map>)eduObj.get("concentration");
        if(concentrationObj != null)
        {
            for(Map conc: concentrationObj) {
                //Taking 1st branch/stream of studies
                concentration = (String)conc.get("name");
                break;
            }
        }

        if(eduObj.containsKey("type"))
            type = (String)eduObj.get("type");
    }


    public String getSchool() {
        return school;
    }

    public void setSchool(String school) {
        this.school = school;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getConcentration() {
        return concentration;
    }

    public void setConcentration(String concentration) {
        this.concentration = concentration;
    }
}
