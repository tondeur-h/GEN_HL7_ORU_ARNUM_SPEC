package org.dev.ht;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * ******************
 * @author tondeur-h
 * @version mai 2015
 ********************
 */
/**
 * *******************************************
 * Classe de gestion des fichier HL7
 * Spécialisé dans le format ORU pour Visucourrier
 * Elle permet de genérer un fichier HL7 ORU
 * et si nécessaire un fichier verrou en extension .ok
 ********************************************
 */
public class Hl7ORU {

 //format du fichier de sortie HL7 ORU vers la base de données.
//ORU out
//MSH10 Message control ID (R)
    private String ORUMSH = "MSH|^~\\&|#SENDAPPLI#|#SENDAPPLI#|CHVDEMAT|ARNUM|#DATEMSG#||ORU^R01|#MSGCTRLID#|P|2.3.1||||||8859/1";
//PID 18 Patient control Account
    private String ORUPID = "PID|||#IPP#||#NOM#^#PRENOM#||#DATEN#|#SEXE#||||||||||#IEP#";
//PV1
    private String ORUPV1 = "PV1|1||#UF#||||||||||||||||#IEP#";
//OBR REFNOMBER(0217Hl7) OBR7=Date du document
    private String ORUOBR = "OBR|1||#REFNUMBER#|#EVENTCD#^#EVENTTITLETEXT#|||#DATEDOC#|||||||||#AUTEUR#^^||||||||DOC|F||1^once^^#DATEDOC#^^R|||||";
//OBX (EVENTCD=alias Cerner), (EVENTTITLETEXT=commentaire du doc), (SECURITYDOC=0 doc Normal ou 3 doc securisé), (DATEDOC=date de création du doc)
    private String ORUOBX = "OBX|1|RP|#EVENTCD#^^^^#EVENTTITLETEXT#||#URL#^URL^HTML^URL||||||||#SECURITEDOC#|#DATEDOC#";


    /*****************************
     * VARIABLES GLOBALES
     ****************************/
    //identité patient et data patient
    private String NOM;
    private String PRENOM;
    private String NOMJF;
    private String SEXE;
    private String IPP;
    private String IEP;
    private String DATEN;
    private String URL; //nom du doc
    private String DATEIN;
    private String DATEDOC;
    private String NUMDOC;
    private String TYPEEXAM; //EVENTITLETEXT
    private String UF;
    private String AUTEUR = "CHVDemat";
    private String EVENTCD = ""; //alias Cerner
    private String MSGCTRLID = "msg1"; //MSH10 numero de message
    private String SENDAPPLI = "CHV_DEMAT"; //MSH3 & 4
    private String SECURITEDOC="0"; //securite du document 0 par defaut...

    //newlines
    private final int CR = 13;
    private final int LF = 10;

    //Constantes
    public final String SECNORMAL="0";
    public final String SECPSY="3";
    public final String SECORTHO="4";
    public final String SECVIH="5";
    public final String SECGENETIQUE="6";
    public final String SECAUTRE="9";

    //logger par les api java
    static final Logger logger = Logger.getLogger(Hl7ORU.class.getName());


    /*************************************************************
     * constructeur initialisation
     * initialise surtout les variables
     * @param NOM String
     * @param PRENOM String
     * @param NOMJF String
     * @param SEXE String
     * @param IPP String
     * @param IEP String
     * @param DATEN String
     * @param URL String
     * @param NUMDOC String
     * @param DATEIN String
     * @param DATEDOC String
     * @param ALIAS String
     * @param TYPEEXAM String
     * @param UF String
     * @param AUTEUR String
     * @param SECCURITEDOC
     *************************************************************/
    public Hl7ORU(String NOM, String PRENOM, String NOMJF, String SEXE, String IPP, String IEP, String DATEN, String URL, String NUMDOC, String DATEIN, String DATEDOC, String ALIAS, String TYPEEXAM, String UF, String AUTEUR, String SECCURITEDOC) {
        //parametrage du logger pour generer des logs
        try
        {
            logger.setUseParentHandlers(false);
            FileHandler fh = new FileHandler("HL7ORU%u.log", 0, 1, true);
            fh.setFormatter(new SimpleFormatter());
            logger.addHandler(fh);
        } catch (IOException ioe) {
            //erreur silencieuse...
        }

        this.NOM = NOM;
        this.PRENOM = PRENOM;
        this.NOMJF = NOMJF;
        this.SEXE = SEXE;
        this.IPP = IPP;
        this.IEP = IEP;
        this.DATEN = DATEN;
        this.URL = URL;
        this.DATEIN = DATEIN;
        this.DATEDOC = DATEDOC;
        this.NUMDOC = NUMDOC;
        this.EVENTCD = ALIAS;
        this.TYPEEXAM = TYPEEXAM;
        this.UF = UF;
        this.AUTEUR = AUTEUR;
        this.SECURITEDOC = SECCURITEDOC;
    }


    /*****************************************************
     * convertir une date JJ/MM/AAAA en date hl7 AAAAMMJJ
     *
     *
     * @param dateSimple String
     * @return String
     *****************************************************/
    public String Date_to_Date_HL7_court(String dateSimple) {
//parser la date
        String JJ = dateSimple.substring(0, 2);
        String MM = dateSimple.substring(3, 5);
        String AAAA = dateSimple.substring(6, 10);
        return AAAA + MM + JJ + "000000";
    }


    /*********************************************************************
     * convertir une date JJ/MM/AAAA HH:MM:SS en date hl7 AAAAMMJJHHMMSS
     *
     *
     * @param dateComplexe String
     * @return String
     ********************************************************************/
    public String Date_to_Date_HL7_long(String dateComplexe) {
    //parser la date
        String JJ = dateComplexe.substring(0, 2);
        String MM = dateComplexe.substring(3, 5);
        String AAAA = dateComplexe.substring(6, 10);
        String HH = dateComplexe.substring(11, 13);
        String mm = dateComplexe.substring(14, 16);
        return AAAA + MM + JJ + HH + mm + "00";
    }


    /*********************************************************************
     * Fonction de remplacement des tag #XXXX# pour les segments HL7
     *
     * @param in (Chaine de caratere du segment a remplacer
     * @return String
     * @throws java.lang.Exception Erreur replaceAll
     *********************************************************************/
    private String hl7(String in) throws Exception {
        String outS = in;
//Segment MSH
        outS = outS.replaceAll("#SENDAPPLI#", SENDAPPLI);
        outS = outS.replaceAll("#MSGCTRLID#", MSGCTRLID);
        outS = outS.replaceAll("#DATEMSG#", Date_to_Date_HL7_long(DATEIN));
//segment PID/PV1
        outS = outS.replaceAll("#NOM#", NOM);
        outS = outS.replaceAll("#PRENOM#", PRENOM);
        outS = outS.replaceAll("#SEXE#", SEXE);
        outS = outS.replaceAll("#DATEN#", Date_to_Date_HL7_court(DATEN));
        outS = outS.replaceAll("#IPP#", IPP);
        outS = outS.replaceAll("#IEP#", IEP);
        outS = outS.replaceAll("#UF#", UF);

//segment OBR/OBX
        outS = outS.replaceAll("#AUTEUR#", AUTEUR);
        outS = outS.replaceAll("#REFNUMBER#", NUMDOC);
        outS = outS.replaceAll("#DATEDOC#", Date_to_Date_HL7_long(DATEDOC));
        outS = outS.replaceAll("#EVENTCD#", EVENTCD);
        outS = outS.replaceAll("#EVENTTITLETEXT#", TYPEEXAM);
        outS = outS.replaceAll("#URL#", URL);
        outS = outS.replaceAll("#SECURITEDOC#", SECURITEDOC);
        return outS;
    }


    /***************************************************
     * Procedure qui permet d'ecrire le fichier hl7_oru
     *
     *
     * @param outputFileName String
     * @return boolean
     * @throws java.lang.Exception Erreur d'ecriture sur disque
     ***************************************************/
    public boolean create_oru(String outputFileName) throws Exception {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(outputFileName));
            out.write(hl7(ORUMSH));
            out.write(CR);
            out.write(hl7(ORUPID));
            out.write(CR);
            out.write(hl7(ORUPV1));
            out.write(CR);
            out.write(hl7(ORUOBR));
            out.write(CR);
            out.write(hl7(ORUOBX));
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (out != null) {
                out.close();
            }
        }
        return true;
    } //fin create_oru


    /**********************************************
     * Creer un fichier verrou avec extension .ok 
     * seulement si demandé dans le parametrage
     *
     * @param outputFileName String
     * @return boolean
     * @throws Exception Erreur d'ecriture sur disque
     **********************************************/
    public boolean create_OK(String outputFileName) throws Exception {
        PrintWriter out = null;
        try {
            out = new PrintWriter(new FileWriter(outputFileName));
            out.print(outputFileName);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (out != null) {
                out.close();
            }
        }
        return true;
    } //fin create_OK

} //fin de la classe
