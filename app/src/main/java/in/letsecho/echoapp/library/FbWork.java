package in.letsecho.echoapp.library;

import java.util.Map;

public class FbWork {

    private String employer;
    private String position;
    private String location;
    private String start_date;
    private String end_data;

    public FbWork() {}

    public FbWork(Map workObj) {
        Map empObj =  (Map)workObj.get("employer");
        if(empObj != null)
            employer = (String)empObj.get("name");


        Map positionObj = (Map)workObj.get("position");
        if(positionObj != null)
            position = (String)positionObj.get("name");

        Map locationObj = (Map)workObj.get("location");
        if(locationObj != null)
            location = (String)locationObj.get("name");

        if(workObj.containsKey("start_date"))
            start_date = (String)workObj.get("start_date");

        if(workObj.containsKey("end_date"))
            end_data = (String)workObj.get("end_date");
    }

    public String getEmployer() {
        return employer;
    }

    public void setEmployer(String employer) {
        this.employer = employer;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStart_date() {
        return start_date;
    }

    public void setStart_date(String start_date) {
        this.start_date = start_date;
    }

    public String getEnd_data() {
        return end_data;
    }

    public void setEnd_data(String end_data) {
        this.end_data = end_data;
    }
}