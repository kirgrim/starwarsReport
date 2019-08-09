package com.controller;
import com.db_session.HibernateUtil;
import com.model.Report;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class ReportController {
    public static String getJSONFromURL(String url_name) throws IOException, JSONException {
        URL url=new URL(url_name);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'GET' request to URL : " + url_name);
        System.out.println("Response Code : " + responseCode);
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        //print in String
        System.out.println(response.toString());
        //Read JSON response and print
        String response_str=response.toString();
        response_str=response_str.replace("\\r\\n","");
        return response_str;
    }

    @RequestMapping(value = "/report/{report_id}",method = RequestMethod.PUT)
    public void put_report(@PathVariable Integer report_id,@RequestBody JSONObject criteria) throws IOException, JSONException {
        Session s = HibernateUtil.getSessionFactory().openSession();
        System.out.println("getting query criteria");
        final String query_criteria_character_phrase;
        final String query_criteria_planet_name;
        query_criteria_character_phrase=criteria.getString("query_criteria_character_phrase");
        query_criteria_planet_name=criteria.getString("query_criteria_planet_name");
//        Criteria cr = s.createCriteria(Report.class);
//        cr.add(Restrictions.like("character_name",query_criteria_character_phrase, MatchMode.ANYWHERE));
//        cr.add(Restrictions.eq("planet_name",query_criteria_planet_name));
//        cr.setMaxResults(1);
//        List<Report> result =cr.list();
        String films=getJSONFromURL("https://swapi.co/api/films/?format=json");
        String people=getJSONFromURL("https://swapi.co/api/people/?format=json");
        String planets=getJSONFromURL("https://swapi.co/api/planets/?format=json");
        JSONObject filmsJSON=new JSONObject(films);
        JSONObject planetJSON = new JSONObject(planets);
        JSONObject peopleJSON=new JSONObject(people);
        JSONArray jarrPeople=peopleJSON.getJSONArray("results");
        JSONArray jarrPlanet=planetJSON.getJSONArray("results");
        JSONArray jarrFilms=filmsJSON.getJSONArray("results");
        List<String> people_names=new ArrayList();
        List<Integer> people_ids=new ArrayList<>();
        List<String> planet_urls=new ArrayList<>();
        List<String> film_urls=new ArrayList<>();
        String planetName = "";
        String planetURL = "";
        Integer planetID = null;
        String characterName = "";
        Integer characterID = null;
        String filmName = "";
        Integer filmID = null;
        for(int i=0;i<jarrPeople.length();i++) {
            people_names.add(jarrPeople.getJSONObject(i).getString("name"));
        }
        for(String name:people_names){
            if(!name.contains(query_criteria_character_phrase))
                people_names.remove(name);
        }
        for(int j=0;j<jarrPeople.length();j++){
            for(String name:people_names) {
                if (jarrPeople.getJSONObject(j).getString("name").equals(name)){
                    people_ids.add(Character.getNumericValue(jarrPeople.getJSONObject(j).getString("url").charAt(jarrPeople.getJSONObject(j).getString("url").length()-2)));
                    planet_urls.add(jarrPeople.getJSONObject(j).getString("homeworld"));
                }
            }
        }
        for(int i=0;i<jarrPlanet.length();i++){
            for(String planet_url:planet_urls){
                if(jarrPlanet.getJSONObject(i).getString("url").equals(planet_url)&&jarrPlanet.getJSONObject(i).getString("name").equals(query_criteria_planet_name)){
                    planetName=jarrPlanet.getJSONObject(i).getString("name");
                    planetURL=jarrPlanet.getJSONObject(i).getString("url");
                    planetID=Character.getNumericValue(jarrPlanet.getJSONObject(i).getString("url").charAt(jarrPlanet.getJSONObject(i).getString("url").length()-2));
                }
            }
        }
        for(int i=0;i<jarrPeople.length();i++){
            for(String people_name:people_names){
                if(jarrPeople.getJSONObject(i).getString("homeworld").equals(planetURL)){
                    characterName=people_name;
                    characterID=people_ids.get(people_names.indexOf(people_name));
                }
            }
        }

        List film_names=new ArrayList();
        List film_ids=new ArrayList();
        for(int i=0;i<jarrFilms.length();i++){
            JSONArray characters=jarrFilms.getJSONObject(i).getJSONArray("characters");
            for(int j=0;j<characters.length();j++){
                int k=Character.getNumericValue(characters.get(j).toString().charAt(characters.get(i).toString().length()-2));
                if(k==characterID){
                    filmName=jarrFilms.getJSONObject(i).getString("title");
                    filmID=jarrFilms.getJSONObject(i).getInt("episode_id");
                }
            }

        }
        Report result=new Report();
        result.setCharacter_id(characterID);
        result.setCharacter_name(characterName);
        result.setFilm_id(filmID);
        result.setFilm_name(filmName);
        result.setPlanet_id(planetID);
        result.setPlanet_name(planetName);
        Transaction tx=s.beginTransaction();
        Query checkReportByID=s.createQuery("select report_id from com.model.Report where report_id=:x");
        checkReportByID.setParameter("x",report_id);
        List<Integer> ids=checkReportByID.list();
        tx.commit();
        Query change;
        if(ids.isEmpty()){
            change = s.createSQLQuery("INSERT INTO com.model.Report values(:id,:character_criteria,:planet_criteria,:film_id,:film_name,:chr_id,:chr_name,:plnt_id,:plnt_name)");
        }
        else {
            change=s.createQuery("UPDATE com.model.Report SET character_criteria=:character_criteria,planet_criteria=:planet_criteria,film_id=:film_id,film_name=:film_name,character_id=:chr_id,character_name=:chr_name,planet_id=:plnt_id,planet_name=:plnt_name where report_id=:id");
        }
        change.setParameter("id",report_id);
        change.setParameter("character_criteria",query_criteria_character_phrase);
        change.setParameter("planet_criteria",query_criteria_planet_name);
        change.setParameter("film_id",result.getFilm_id());
        change.setParameter("film_name",result.getFilm_name());
        change.setParameter("chr_id",result.getCharacter_id());
        change.setParameter("chr_name",result.getCharacter_name());
        change.setParameter("plnt_id",result.getPlanet_id());
        change.setParameter("plnt_name",result.getPlanet_name());
        change.executeUpdate();
    }

    @RequestMapping(value = "/report/{report_id}",method = RequestMethod.DELETE)
    public void delete_report(@PathVariable Integer report_id) {
        Session s = HibernateUtil.getSessionFactory().openSession();
        Transaction tx=s.beginTransaction();
        Query deleteReportByID=s.createQuery("delete from com.model.Report where report_id=:x");
        deleteReportByID.setParameter("x",report_id);
        int status=deleteReportByID.executeUpdate();
        System.out.println(status);
        tx.commit();
    }
    @RequestMapping(value = "/report",method = RequestMethod.DELETE)
    public void delete_all_reports() {
        Session s = HibernateUtil.getSessionFactory().openSession();
        Transaction tx=s.beginTransaction();
        Query deleteReports=s.createQuery("delete from com.model.Report");
        int status=deleteReports.executeUpdate();
        System.out.println(status);
        tx.commit();
    }
    @RequestMapping(value = "/report",method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public List get_reports() {
        Session s = HibernateUtil.getSessionFactory().openSession();
        Transaction tx=s.beginTransaction();
        Query checkReportByID = s.createQuery("from com.model.Report");
        List reports = checkReportByID.list();
        try {
            tx.commit();
        }catch (Exception e){
            System.out.println("transaction failed");
            tx.rollback();
            throw e;
        }
        return reports;
    }
    @RequestMapping(value = "/report/{report_id}",method = RequestMethod.GET,produces = "application/json")
    public Object get_report(@PathVariable Integer report_id) throws IOException, JSONException {
        Session s = HibernateUtil.getSessionFactory().openSession();
        Transaction tx=s.beginTransaction();
        Query checkReportByID=s.createQuery("from com.model.Report where report_id=:x");
        checkReportByID.setParameter("x",report_id);
        List reports = checkReportByID.list();
        tx.commit();
        return reports.get(0);
    }
}
