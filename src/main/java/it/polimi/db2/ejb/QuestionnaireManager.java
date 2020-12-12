package it.polimi.db2.ejb;

import it.polimi.db2.entities.*;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.persistence.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
public class QuestionnaireManager {

    @PersistenceContext(unitName = "projectPersistenceUnit")
    private EntityManager em;

    private List<MarketingQuestionEntity> marketingQuestionEntityList;
    private HashMap< StatisticalQuestionEntity, List<StatQuestionAlternativesEntity> >  statisticalQuestionEntityList;

    public QuestionnaireManager () {}


    @PostConstruct
    public void init(){

        this.marketingQuestionEntityList = retrieveMarketingQuestions();
        this.statisticalQuestionEntityList = retrieveStatisticalQuestions();

    }


    public HashMap< StatisticalQuestionEntity, List<StatQuestionAlternativesEntity> >  getStatisticalQuestionEntityList() {
        return statisticalQuestionEntityList;
    }



    //TBD
    //to be used together with UserManager's banUser!
    //returns true if the text of an answer contains one of the forbidden words
    public boolean checkForOffensiveWords(HashMap<Integer, String> mapMarketingAnsQuest) {
        for (String s : mapMarketingAnsQuest.values()) {
            if( checkSingleAnswer(s) ){




                return true;
            }
        }

        return false;
    }

    public List<MarketingQuestionEntity> getMarketingQuestionEntityList() {
        return marketingQuestionEntityList;
    }

    private boolean checkSingleAnswer(String answerText) {


        int numOfForbidden = 0;
        ForbiddenWordsEntity currentForbidden = new ForbiddenWordsEntity();

        try {
            long l = (long) em.createQuery("SELECT count(i) from ForbiddenWordsEntity i").getSingleResult();
            numOfForbidden = (int) l;

        }catch (PersistenceException e) {
            e.printStackTrace();
        }



        for(int i=0; i<numOfForbidden; i++){

            try {
                currentForbidden = em.createNamedQuery("ForbiddenWordN", ForbiddenWordsEntity.class)
                        .setParameter(1, i).getSingleResult();

            }catch (PersistenceException e) {
                e.printStackTrace();
            }

            if (answerText.contains(currentForbidden.getForbiddenWord())) {
                return true;
            }
        }

        return false;

    }


    //la query sul prodotto non ha where sulla data!!!!!
    public List<MarketingQuestionEntity> retrieveMarketingQuestions(){


        Date today = Date.valueOf(LocalDate.now());

        try{
            //da aggiungere eccezione se ritorna più prodotti
            /*List<ProductEntity> listOfProduct = em.createNamedQuery("ProductEntity.getProductOfTheDay", ProductEntity.class)
                    .setParameter("today",today, TemporalType.DATE).getResultList();*/


            List<ProductEntity> listOfProduct = em.createQuery("SELECT r FROM ProductEntity r")
                    .getResultList();


            List<MarketingQuestionEntity> listMQ = listOfProduct.get(0).getQuestionnaire().getmList();


            if(listOfProduct.size() != 1){
                //eccezione da tirare
            }


            else{

                return listMQ;

            }

        }catch (PersistenceException ex){

            ex.printStackTrace();
        }


        return null;


    }

    public HashMap< StatisticalQuestionEntity, List<StatQuestionAlternativesEntity> > retrieveStatisticalQuestions(){

        try{

            HashMap<StatisticalQuestionEntity, List<StatQuestionAlternativesEntity> > mapStatQuestAlternatives = new HashMap<>();

            List<StatisticalQuestionEntity> listOfQuestions = em.createQuery("SELECT r FROM StatisticalQuestionEntity r")
                    .getResultList();


            for(StatisticalQuestionEntity sqe : listOfQuestions){


              List<StatQuestionAlternativesEntity>  listOfAlt = em.createQuery("SELECT u FROM StatQuestionAlternativesEntity u WHERE u.statisticalQuestion.idStatisticalQuestion = ?1")
                        .setParameter(1, sqe.getIdStatisticalQuestion()).getResultList();


              mapStatQuestAlternatives.put(sqe, listOfAlt);


            }

            return mapStatQuestAlternatives;


        }catch (PersistenceException ex){

            ex.printStackTrace();
        }


        return null;

    }


    public void persistQuestionnaireAnswers( List<MarketingAnswerEntity> mAnsList, List<StatisticalAnswerEntity> sAnsList, UserEntity user){

        QuestionnaireEntity todaysQuestionnaire = mAnsList.get(0).getmQuestion().getQuestionnaire();
        QuestionnaireResponseEntity completedTheQuestionnaire = new QuestionnaireResponseEntity(todaysQuestionnaire, user, new Byte("00000000"));

        try {
            for(MarketingAnswerEntity answer: mAnsList) {
                em.persist(answer);

            }

            for (StatisticalAnswerEntity answer: sAnsList) {
                em.persist(answer);
            }

            em.persist(completedTheQuestionnaire);
            em.flush();

        }catch(PersistenceException e) {
            e.printStackTrace();
        }

    }


    //support method for checkForOffensiveWords
    //probably won't be needed for all
    public List<String> convertToStringList (List<ForbiddenWordsEntity> fWList) {
        List<String> sList = null;
        sList = fWList.stream().map(obj -> obj.getForbiddenWord()).collect(Collectors.toList());

        return sList;
    }

    //when he uses offensive words
    public void banUser (UserEntity user) {

        try {
            user= em.find(UserEntity.class, user.getIdUser());
            user.setFlagStatus(new Byte("00000001"));
            em.flush();

        }catch(PersistenceException e) {
            e.printStackTrace();
        }
    }

}
