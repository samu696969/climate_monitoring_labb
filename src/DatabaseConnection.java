import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseConnection {

    private static final String URL = "jdbc:postgresql://localhost:5432/postgres";
    private static final String USER = "postgres";
    private static final String PASSWORD = "labb18";
    private  static OperatoreRegistrato op;
    public static Connection connect() {
        Connection conn = null;
        try {
            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connessione al database stabilita!");
        } catch (SQLException e) {
            System.err.println("Errore di connessione: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("Driver non trovato: " + e.getMessage());
        }
        return conn;
    }

    // Metodo per creare un nuovo operatore registrato, ritorna l' id generato se l'operazione è andata a buon fine , altrimenti -1 , -2 ( se l'utente ha inserito codice fiscale / user id gia esistenti ) , -3 se si è inserito un centro_id non esistente
    public synchronized OperatoreRegistrato createOperatoreRegistrato(String nome, String cognome, String codiceFiscale, String email, String userid, String password, Integer centroMonitoraggio_id) {
        // controlla che non esistano gia registrato questi valori , sono campi unique
        if (recordExists(codiceFiscale, userid)) {
            System.err.println("Errore: Il codice fiscale o l'userid esistono già.");
            return new OperatoreRegistrato(-2); //codice errore per codicefiscale o userid già esistenti:vincolo unique
        }

        // se il centro di monitoraggio è fornito , controllare se esiste
        if (centroMonitoraggio_id != null && centroMonitoraggio_id > 0 && !centroMonitoraggioExists(centroMonitoraggio_id)) {
            System.err.println("Errore: Il centro di monitoraggio con ID " + centroMonitoraggio_id + " non esiste.");
            return new OperatoreRegistrato(-3); //codice errore per centro di monitoraggio non esistente
        }
        // Controllo che l'email contenga il simbolo @
        if (email == null || !email.contains("@")) {
            System.err.println("Errore: Email non valida, deve contenere il simbolo '@'.");
            return new OperatoreRegistrato(-4);  // Codice errore per email non valida
        }

        // Controllo che il codice fiscale abbia esattamente 16 caratteri
        if (codiceFiscale == null || codiceFiscale.length() != 16) {
            System.err.println("Errore: Il codice fiscale deve essere esattamente di 16 caratteri.");
            return new OperatoreRegistrato(-5);  // Codice errore per codice fiscale non valido
        }
        //controllo che siano stati forniti valori non nulli per la consistenza dei dati
        if(nome == null || cognome == null || codiceFiscale == null || email==null || userid==null || password==null ){
            System.err.println("i campi sono nulli");
            return new OperatoreRegistrato(-6); //codice errore per campi nulli : vincolo NOT NULL
        }

        String sql = "INSERT INTO OperatoriRegistrati (nome, cognome, codice_fiscale, email, userid, password, centro_monitoraggio_id) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, nome);
            pstmt.setString(2, cognome);
            pstmt.setString(3, codiceFiscale);
            pstmt.setString(4, email);
            pstmt.setString(5, userid);
            pstmt.setString(6, password);

            if (centroMonitoraggio_id != null && centroMonitoraggio_id > 0) {
                pstmt.setInt(7, centroMonitoraggio_id);
            } else {
                pstmt.setNull(7, java.sql.Types.INTEGER);
            }

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int operatoreId = generatedKeys.getInt(1);
                        System.out.println("Operatore registrato con successo! ID: " + operatoreId);
                         op= new OperatoreRegistrato(operatoreId,centroMonitoraggio_id);
                        return op;

                    }
                }
            }

        } catch (SQLException e) {
            System.out.println("Errore durante la registrazione dell'operatore: " + e.getMessage());
            e.printStackTrace();
            return new OperatoreRegistrato(-1);
        }

        return new OperatoreRegistrato(-1);
    }

//metodo per verificare se un area di interesse inserita nei centri monitoraggio esiste
    private boolean areeInteresseExist(String [] elencoaree){
        boolean esiste;
        for (int i=0;i< elencoaree.length;i++ ) {
            esiste = areaSpecificaExists(elencoaree[i]);
            if (esiste == false) {

                return false ;
            };

        }
        return true;
    }

    private boolean areaSpecificaExists(String area){
        String sql= "SELECT COUNT(*) FROM CoordinateMonitoraggio WHERE denominazione_ufficiale ILIKE ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, area);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                boolean areaesiste= rs.getInt(1) > 0;
                if (areaesiste) {
                    return true;
                }
                else
                    System.err.println("area: " + area + " non esistente , provvedi eventualmente a registrare l'area con le sue coordinate ");
                return false;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return false;





    }
    //metodo per verificare se l'operatore esiste nel database
    private boolean operatoreExists(int operatore_id){
        String sql= "SELECT COUNT(*) FROM OperatoriRegistrati WHERE id=?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, operatore_id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;


    }
    // Metodo per verificare se un centro di monitoraggio esiste
    private boolean centroMonitoraggioExists(int centroMonitoraggio_id) {
        String sql = "SELECT COUNT(*) FROM CentriMonitoraggio WHERE id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, centroMonitoraggio_id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    //controlla che non siano inseriti campi user_id o codice fiscali gia presenti : sono campi unique!
    private boolean recordExists(String codiceFiscale, String userid) {
        String sql = "SELECT COUNT(*) FROM OperatoriRegistrati WHERE codice_fiscale ILIKE ? OR userid ILIKE ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, codiceFiscale);
            pstmt.setString(2, userid);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

//metodo login:restituisce

    public synchronized OperatoreRegistrato loginOperatore(String userid, String password) {
        String sql = "SELECT id, centro_monitoraggio_id FROM OperatoriRegistrati WHERE userid = ? AND password = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userid);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    Integer centro_monitoraggio_id = (Integer) rs.getObject("centro_monitoraggio_id");

                    System.out.println("Login effettuato con successo per l'operatore con ID: " + id +
                            ", Centro Monitoraggio ID: " + (centro_monitoraggio_id != null ? centro_monitoraggio_id : "Non assegnato"));

                    return new OperatoreRegistrato(id, centro_monitoraggio_id);
                } else {
                    System.err.println("Login fallito: userid o password errati.");
                    return new OperatoreRegistrato(-1);
                }
            }

        } catch (SQLException e) {
            System.out.println("Errore nel tentativo di connessione per effettuare il login: " + e.getMessage());
            e.printStackTrace();
            return new OperatoreRegistrato(-2);
        }
    }
    /* Metodo per creare un nuovo centro di monitoraggio associato a un operatore

    N.B: è richiesto l'id dell'operatore per favorire l'aggiornamento dei dati della tabella operatoriregistrati (campo centro_monitoraggio_id)
    *
     */

    /*
    public int createCentroMonitoraggio(String nome, String indirizzo, String[] elencoAreeInteresse, int idOperatore) {
        String sql = "INSERT INTO CentriMonitoraggio (nome, indirizzo, elenco_aree_interesse) VALUES (?, ?, ? )";
        boolean esisteoperatore=operatoreExists(idOperatore);
       //controlla che l'id inserito corrisponda ad un operatore registrato nel databse
        if(!esisteoperatore){
            System.out.println("operatore " + idOperatore + " non esiste");
            return  -2;
        }
        boolean esistearea=areeInteresseExist(elencoAreeInteresse);
        if(!esistearea){
            System.out.println("sono state inserite aree non registrate");
            return -3;
        }
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, nome);
            pstmt.setString(2, indirizzo);
            Array elencoAreeArray = conn.createArrayOf("TEXT", elencoAreeInteresse);
            pstmt.setArray(3, elencoAreeArray);
        //    N.B: il quarto argomento del metodo è necessario per aggiornare l'operatore con un riferimento al centro di monitoraggio che lui stesse sta creando

            pstmt.executeUpdate();
            System.out.println("Centro di monitoraggio creato con successo!");

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int centroId = generatedKeys.getInt(1);
                    System.out.println("ID del centro di monitoraggio generato: " + centroId + " , memorizzalo , ti servirà in seguito per eventuali operazioni ");
                    updateOperatoreCentro(idOperatore, centroId);
                    updateAreaInteresseCentro(elencoAreeInteresse,centroId);
                    return centroId;
                }
            }

        } catch (SQLException e) {
            System.out.println("Errore nella creazione del centro di monitoraggio: " + e.getMessage());  e.printStackTrace();
            return -1; //errore di connessione
        }
        return -1; //errore generale
    }
*/

    public synchronized int createCentroMonitoraggio(String nome, String indirizzo, String[] elencoAreeInteresse, int idOperatore) {
        String sql = "INSERT INTO CentriMonitoraggio (nome, indirizzo, elenco_aree_interesse) VALUES (?, ?, ?)";

        // controlla se l'operatore inserito esiste
        boolean esisteOperatore = operatoreExists(idOperatore);
        if (!esisteOperatore) {
            System.out.println("Errore: l'operatore con ID " + idOperatore + " non esiste");
            return -2;
        }

        //controlla se tutte le aree esistono
        boolean esisteArea = areeInteresseExist(elencoAreeInteresse);
        if (!esisteArea) {
            System.out.println("Errore: sono state inserite aree non registrate");
            return -3; //errore codice 3 , l'utente inserisce un area da rilevare non esistente nel database delle area registrate
        }
       if(nome==null || indirizzo==null ){
           System.err.println("i campi nome , indirizzo ed aree interesse non possono essere vuoti");
           return -4; //errore codice 4 , l'utente ha inserito valori nulli per campi di tipo not null

       }
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, nome);
            pstmt.setString(2, indirizzo);
            Array elencoAreeArray = conn.createArrayOf("TEXT", elencoAreeInteresse);
            pstmt.setArray(3, elencoAreeArray);

            pstmt.executeUpdate();
            System.out.println("Centro di monitoraggio creato con successo!");

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int centroId = generatedKeys.getInt(1);
                    System.out.println("ID del centro di monitoraggio generato: " + centroId + " , memorizzalo, ti servirà in seguito per eventuali operazioni");
                    updateOperatoreCentro(idOperatore, centroId);
                    updateAreaInteresseCentro(elencoAreeInteresse, centroId);
                    op.setCentroMonitoraggioId(centroId);
                    return centroId;
                }
            }

        } catch (SQLException e) {
            if (e.getSQLState().equals("23505")) { // PostgreSQL unique violation error code
                System.out.println("Errore: Un centro di monitoraggio con questo indirizzo esiste già.");
                return -5; // errore causato dal vincolo unique : indirizzo del centro inserito esiste già
            } else {
                System.out.println("Errore durante la creazione del centro di monitoraggio: " + e.getMessage());
                e.printStackTrace();
                return -1; // General error
            }
        }
        return -1; // Should not reach here, but included for completeness
    }
    //

    //modifica il campo centro_monitoraggio di un operatore registrato , dopo che quest'ultimo ha registrato il suo centro di monitoraggio
    public void updateOperatoreCentro(int idOperatore, int centroId) {
        String sql = "UPDATE OperatoriRegistrati SET centro_monitoraggio_id = ? WHERE id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, centroId);
            pstmt.setInt(2, idOperatore);

            pstmt.executeUpdate();
            System.out.println("Operatore aggiornato con il centro di monitoraggio!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
//aggiorna le aree di interesse con il riferimento al centro di monitoraggio che visionerà quella specifica area -> implica che un'area sia monitorata da un e un solo centro
    public void updateAreaInteresseCentro(String [] elencoaree, int centroId ) {

       for(int i=0; i< elencoaree.length; i++){
           String sql= "UPDATE CoordinateMonitoraggio SET centro_riferimento_id = ? WHERE denominazione_ufficiale = ? " ;
           try (Connection conn = connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

               pstmt.setInt(1, centroId);
               pstmt.setString(2, elencoaree[i]);

               pstmt.executeUpdate();
               System.out.println("Area: " + elencoaree[i] + " aggiornato con il centro di monitoraggio di referenza!");

           } catch (SQLException e) {
               e.printStackTrace();
           }


       }





    }
    // Metodo per inserire i parametri climatici inclusa la massa dei ghiacciai: nota bene i parametri di tipo float non possono avere piu di 3 cifre prima della virgola
    public int insertParametriClimatici(int centroMonitoraggio_id, String denominazione_ufficiale_area, int operatore_id, String dataRilevazione,
                                         float velocitaVento, int scoreVento, String notaVento,
                                         float umidita, int scoreUmidita, String notaUmidita,
                                         float pressione, int scorePressione, String notaPressione,
                                         float temperatura, int scoreTemperatura, String notaTemperatura,
                                         float precipitazioni, int scorePrecipitazioni, String notaPrecipitazioni,
                                         float altitudineGhiacciai, int scoreAltitudineGhiacciai, String notaAltitudineGhiacciai,
                                         float massaGhiacciai, int scoreMassaGhiacciai, String notaMassaGhiacciai) {
//per praticità , l'utente deve inserire soltanto la denominazione ufficiale dell'area che monitora , delegando la ricerca dell'id di riferimento al sistema
        int coordinate_monitoraggio_id = get_id_denominazione_area(denominazione_ufficiale_area);
//assicuriamoci che l'utente inserisca un ' area esistente in coordinate monitoraggio
        if (coordinate_monitoraggio_id == -1) {
            System.err.println("Errore: denominazione ufficiale '" + denominazione_ufficiale_area + "' non trovata.");
            return -1;
        }
//assicuriamoci che l'id del centro di monitoraggio esista :
        if(!centroMonitoraggioExists(centroMonitoraggio_id)) {
            System.err.println("Errore: centro monitoraggio " + centroMonitoraggio_id + " non esiste ");
            return -2;
        }
        //assicuriamoci che l'id dell'operatore esista:
        if (!operatoreExists(operatore_id)){
            System.err.println("Errore: operatore " + operatore_id + " non esiste " );
            return -3;
        }

        //controllo che l'operatore lavori effettivamente per il centro che sta inserendo come parametri (integrità dei dati)

        if (!verificaOperatoreCentro(operatore_id, centroMonitoraggio_id)) {
            System.err.println("Errore: l'operatore " + operatore_id + " non è associato al centro di monitoraggio " + centroMonitoraggio_id);
            return -4; // Nuovo codice di errore per questa situazione: l'operatore non lavora per questo centro
        }


//controllo che i valori di score siano compresi tra 1 e 5 : vincolo check

        if(scoreAltitudineGhiacciai>5 || scoreAltitudineGhiacciai<1 || scoreMassaGhiacciai>5 || scoreMassaGhiacciai<1 || scorePrecipitazioni>5 || scorePrecipitazioni<1 || scorePressione>5 || scorePressione<1 || scoreTemperatura>5 || scoreTemperatura<1 || scoreUmidita>5 || scoreUmidita<1 || scoreVento>5 || scoreVento<1 )
        {
            System.out.println("Errore: lo score relativo ai parametri deve essere compreso tra 1 e 5 ");
            return -5;
        }
//vincolo not null su data rilevazione
        if (dataRilevazione == null) {
            System.err.println("Errore: la data di rilevazione non può essere vuota.");
            return -6;
        }

        //consistenza dati di natura ambientale
        if(velocitaVento <= 0 ||  pressione <= 0 ||  precipitazioni < 0 ||  massaGhiacciai <= 0){
            System.err.println("Errore: tutti i parametri climatici devono essere valori positivi.");
            return -7;
        }



        String sql = "INSERT INTO parametriclimatici (centro_monitoraggio_id,coordinate_monitoraggio_id , operatore_id , data_rilevazione, " +
                "velocita_vento, score_vento, nota_vento, umidita, score_umidita, nota_umidita, " +
                "pressione, score_pressione, nota_pressione, temperatura, score_temperatura, nota_temperatura, " +
                "precipitazioni, score_precipitazioni, nota_precipitazioni, altitudine_ghiacciai, " +
                "score_altitudine_ghiacciai, nota_altitudine_ghiacciai, massa_ghiacciai, score_massa_ghiacciai, nota_massa_ghiacciai) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,  ?)";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, centroMonitoraggio_id);
            pstmt.setInt(2, coordinate_monitoraggio_id);  // Questo valore ora è garantito valido
            pstmt.setInt(3, operatore_id); //
            pstmt.setDate(4, Date.valueOf(dataRilevazione));

            // Impostazione dei parametri rimanenti
            pstmt.setFloat(5, velocitaVento);
            pstmt.setInt(6, scoreVento);
            pstmt.setString(7, notaVento);
            pstmt.setFloat(8, umidita);
            pstmt.setInt(9, scoreUmidita);
            pstmt.setString(10, notaUmidita);
            pstmt.setFloat(11, pressione);
            pstmt.setInt(12, scorePressione);
            pstmt.setString(13, notaPressione);
            pstmt.setFloat(14, temperatura);
            pstmt.setInt(15, scoreTemperatura);
            pstmt.setString(16, notaTemperatura);
            pstmt.setFloat(17, precipitazioni);
            pstmt.setInt(18, scorePrecipitazioni);
            pstmt.setString(19, notaPrecipitazioni);
            pstmt.setFloat(20, altitudineGhiacciai);
            pstmt.setInt(21, scoreAltitudineGhiacciai);
            pstmt.setString(22, notaAltitudineGhiacciai);
            pstmt.setFloat(23, massaGhiacciai);
            pstmt.setInt(24, scoreMassaGhiacciai);
            pstmt.setString(25, notaMassaGhiacciai);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.err.println("Parametri climatici inseriti con successo!");
                return 1;
            } else {
                System.err.println("Nessuna riga inserita.");
                return -8; //errore inserimento di origine non specifica
            }

        } catch (SQLException e) {
            System.err.println("Errore nell'inserimento di parametri : " + e.getMessage());  e.printStackTrace();
            return -8;
        }

    }

    //metodo per ottenere l'id della denominazione_ufficiale dell'area , metodo svolto a semplificare l'attività di inserimento di parametri climatici , l'operatore non è cosi costretto a ricordare l'd di ogni area di interesse
    public int get_id_denominazione_area(String denominazione_ufficiale_area) {
        int risultato = -1;
        String sql = "SELECT id FROM coordinatemonitoraggio WHERE denominazione_ufficiale = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, denominazione_ufficiale_area);
            ResultSet rs = pstmt.executeQuery();

            // Sposta il cursore sulla prima riga del ResultSet
            if (rs.next()) {
                risultato = rs.getInt("id");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return risultato;
    }

    //verifica che nel momento in cui un operatore inserisce le informazioni della rilevazione , quest'ultimo lavori effettivamente per il centro di cui vuole riporatare i dati della rilevazione

    private boolean verificaOperatoreCentro(int operatore_id, int centroMonitoraggio_id) {
        String sql = "SELECT COUNT(*) FROM operatoriregistrati WHERE id = ? AND centro_monitoraggio_id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, operatore_id);
            pstmt.setInt(2, centroMonitoraggio_id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.out.println("Errore durante la verifica dell'associazione operatore-centro: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }


    public int insertCoordinateMonitoraggio(float latitudine, float longitudine,
                                            String denominazioneUfficiale, String stato) {
        String sql = "INSERT INTO CoordinateMonitoraggio (latitudine, longitudine, denominazione_ufficiale, stato) " +
                "VALUES (?, ?, ?, ?)";

        if( denominazioneUfficiale==null || stato==null){
            System.err.println("i campi denominazione ufficiale e stato devono essere non nulli");
            return -2;
        }
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setFloat(1, latitudine);
            pstmt.setFloat(2, longitudine);
            pstmt.setString(3, denominazioneUfficiale);
            pstmt.setString(4, stato);

            pstmt.executeUpdate();
            System.out.println("Coordinate di monitoraggio inserite con successo!");

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int coordinatemonitoraggio_id = generatedKeys.getInt(1);
                    System.out.println("ID delle coordinate di monitoraggio generato: " + coordinatemonitoraggio_id);
                    return coordinatemonitoraggio_id;
                }
            }
        } catch (SQLException e) {
            if (e.getSQLState().equals("23505")) { // PostgreSQL unique violation error code : vincolo unique su denominazione ufficiale
                System.err.println("Errore: Coordinate di monitoraggio già esistenti per questa denominazione ufficiale ");
            } else {
                System.err.println("Errore nell'inserimento delle coordinate di monitoraggio: " + e.getMessage());
            }
            e.printStackTrace();
        }
        return -1; // Indicates failure: non è rispettato il vincolo unique
    }

    public synchronized List<AreaGeografica> cercaAreaGeograficaPerDenominazione(String denominazione_ufficiale) {
        List<AreaGeografica> risultati = new ArrayList<>();
        String sql = "SELECT * FROM CoordinateMonitoraggio WHERE denominazione_ufficiale ILIKE ? ";

        try {
            Connection conn = connect();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, denominazione_ufficiale);
            ResultSet rs = pstmt.executeQuery();
            String ris=null;
            while (rs.next()) {
                AreaGeografica area = new AreaGeografica(
                        rs.getString("denominazione_ufficiale"),
                        rs.getString("stato"),
                        rs.getDouble("latitudine"),
                        rs.getDouble("longitudine")
                );
                risultati.add(area);

            }
//lista vuota se non esistono area con con determinata denominazione
        } catch (SQLException e) {
            e.printStackTrace();
        }
return risultati;

    }

    public synchronized List<AreaGeografica> cercaAreaGeograficaPerStato(String stato) {
        List<AreaGeografica> risultati = new ArrayList<>();
        String sql = "SELECT * FROM CoordinateMonitoraggio WHERE stato ILIKE ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, stato);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                AreaGeografica area = new AreaGeografica(
                        rs.getString("denominazione_ufficiale"),
                        rs.getString("stato"),
                        rs.getDouble("latitudine"),
                        rs.getDouble("longitudine")
                );
                risultati.add(area);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        //lista vuota se non ci sono risultati : il server o il client deveono controllare se la lista è vuota
        return risultati;
    }









    // Ricerca per coordinate geografiche
    public synchronized List<AreaGeografica> cercaPerCoordinate(double latitudine, double longitudine) {
        List<AreaGeografica> risultati = new ArrayList<>();
        //serve a selezionare la riga dalla tabella CoordinateMonitoraggio con le coordinate più vicine a un punto specifico
        String sql = "SELECT * " +
                "FROM CoordinateMonitoraggio " +
                "ORDER BY (latitudine - ?)^2 + (longitudine - ?)^2 " +
                "LIMIT 1";  //Limita il numero di righe restituite dalla query a 1
//ORDER BY (latitudine - ?)^2 + (longitudine - ?)^2 ordina i risultati in base a questa distanza quadratica, dalla più vicina alla più lontana
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, latitudine);
            pstmt.setDouble(2, longitudine);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {

 AreaGeografica area= new AreaGeografica
         (rs.getString("denominazione_ufficiale"),rs.getString("stato"),
                 rs.getDouble("latitudine"),rs.getDouble("longitudine"));

             risultati.add(area);

            }

        } catch (SQLException e) {
            e.printStackTrace();

        }
        return risultati;
    }

    // Metodo per visualizzare le informazioni climatiche di un'area di interesse, ILIKE permette di ignorare la differenza tra minuscole e maiuscole
    // Metodo per visualizzare le informazioni climatiche di un'area di interesse




    public synchronized List<ParametriClimatici> visualizzaDatiClimatici(String areaInteresse) {
        List<ParametriClimatici> risultati = new ArrayList<>();

        // Verifica se l'area esiste prima di eseguire la query principale
        int id_area_ricercata = get_id_denominazione_area(areaInteresse);
        if (id_area_ricercata == -1) {
            // L'area non esiste, restituisci una lista vuota
            return risultati;
        }

        String sql = "SELECT " +
                "STRING_AGG(TO_CHAR(data_rilevazione, 'YYYY-MM-DD'), '; ') AS date_rilevazioni, " +
                "AVG(velocita_vento) AS media_velocita_vento, AVG(score_vento) AS score_medio_vento , COUNT(velocita_vento) AS num_vento, STRING_AGG(nota_vento, '; ') AS note_vento, " +
                "AVG(umidita) AS media_umidita, AVG(score_umidita) AS score_medio_umidita , COUNT(umidita) AS num_umidita, STRING_AGG(nota_umidita, '; ') AS note_umidita, " +
                "AVG(pressione) AS media_pressione, AVG(score_pressione) AS score_medio_pressione , COUNT(pressione) AS num_pressione, STRING_AGG(nota_pressione, '; ') AS note_pressione, " +
                "AVG(temperatura) AS media_temperatura, AVG(score_temperatura) AS score_medio_temperatura , COUNT(temperatura) AS num_temperatura, STRING_AGG(nota_temperatura, '; ') AS note_temperatura, " +
                "AVG(precipitazioni) AS media_precipitazioni, AVG(score_precipitazioni) AS score_medio_precipitazioni , COUNT(precipitazioni) AS num_precipitazioni, STRING_AGG(nota_precipitazioni, '; ') AS note_precipitazioni, " +
                "AVG(altitudine_ghiacciai) AS media_altitudine_ghiacciai, AVG(score_altitudine_ghiacciai) AS score_medio_altitudine_ghiacciai , COUNT(altitudine_ghiacciai) AS num_altitudine_ghiacciai, STRING_AGG(nota_altitudine_ghiacciai, '; ') AS note_altitudine_ghiacciai, " +
                "AVG(massa_ghiacciai) AS media_massa_ghiacciai, AVG(score_massa_ghiacciai) AS score_medio_massa_ghiacciai , COUNT(massa_ghiacciai) AS num_massa_ghiacciai, STRING_AGG(nota_massa_ghiacciai, '; ') AS note_massa_ghiacciai " +
                "FROM ParametriClimatici WHERE coordinate_monitoraggio_id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id_area_ricercata);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    ParametriClimatici parametri = new ParametriClimatici(
                            rs.getString("date_rilevazioni"),
                            rs.getFloat("media_velocita_vento"),
                            rs.getFloat("score_medio_vento"),
                            rs.getInt("num_vento"),
                            rs.getString("note_vento"),
                            rs.getFloat("media_umidita"),
                            rs.getFloat("score_medio_umidita"),
                            rs.getInt("num_umidita"),
                            rs.getString("note_umidita"),
                            rs.getFloat("media_pressione"),
                            rs.getFloat("score_medio_pressione"),
                            rs.getInt("num_pressione"),
                            rs.getString("note_pressione"),
                            rs.getFloat("media_temperatura"),
                            rs.getFloat("score_medio_temperatura"),
                            rs.getInt("num_temperatura"),
                            rs.getString("note_temperatura"),
                            rs.getFloat("media_precipitazioni"),
                            rs.getFloat("score_medio_precipitazioni"),
                            rs.getInt("num_precipitazioni"),
                            rs.getString("note_precipitazioni"),
                            rs.getFloat("media_altitudine_ghiacciai"),
                            rs.getFloat("score_medio_altitudine_ghiacciai"),
                            rs.getInt("num_altitudine_ghiacciai"),
                            rs.getString("note_altitudine_ghiacciai"),
                            rs.getFloat("media_massa_ghiacciai"),
                            rs.getFloat("score_medio_massa_ghiacciai"),
                            rs.getInt("num_massa_ghiacciai"),
                            rs.getString("note_massa_ghiacciai")
                    );

                    risultati.add(parametri);
                } else {
                    System.out.println("Nessun dato trovato per l'area di interesse: " + areaInteresse);
                    //viene ritornata una lista vuota
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return risultati;
    }


    public static void main(String[] args) {
        DatabaseConnection dc = new DatabaseConnection();

        // Test di inserimento parametri climatici
        System.out.println("Test di inserimento parametri climatici:");

        // Assumiamo che questi ID esistano nel database
        int centroMonitoraggioId = 1;
        String denominazioneUfficialeArea = "Area Test";
        int operatoreId = 1;

        // Chiamata al metodo di inserimento
        int result = dc.insertParametriClimatici(
                centroMonitoraggioId,
                denominazioneUfficialeArea,
                operatoreId,
                null,  // data rilevazione omessa
                10.5f, 3, "Vento moderato",
                60.0f, 2, "Umidità nella norma",
                1013.25f, 1, "Pressione atmosferica standard",
                22.5f, 2, "Temperatura mite",
                5.0f, 1, "Leggera pioggia",
                3000.0f, 2, "Altitudine ghiacciai nella media",
                1000000.0f, 3, "Massa ghiacciai in diminuzione"
        );

        // Verifica del risultato
        if (result == 1) {
            System.out.println("Inserimento parametri climatici riuscito.");
        } else {
            System.out.println("Errore nell'inserimento dei parametri climatici. Codice errore: " + result);
        }
/*
nel client e/o nel serve vanno implementati dei controlli sui parametri inseriti dall'utente che controllino
 che quest'ultimi non siano null ,
genererebbero errore dato da vincolo not null
Inoltre client e/o server devono controllae che la lista restituita dai vari metodi di visualizzazione o ricerca sia vuota o non ,
nel caso sia vuota significherebbe che la visualizzazione o ricerca non ha dato risultati per i parametri specificati dall'utente
Nel client andrà implementato un controllo che verifichi la compatibilità dei dati , non deve essere possibile inserire
dei valori stringhe laddove èrichiesto un valore di tipo int



 */
    }
}