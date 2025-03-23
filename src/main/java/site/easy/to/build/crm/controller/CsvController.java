// package mg.itu.controller;

// import mg.itu.dto.EspaceDto;
// import mg.itu.dto.OptionDto;
// import mg.itu.dto.PaiementDto;
// import mg.itu.dto.ReservationDto;
// import mg.itu.model.EspaceTravail;
// import mg.itu.model.Option;
// import mg.itu.model.Paiement;
// import mg.itu.model.Reservation;
// import mg.itu.model.Utilisateur;
// import mg.itu.model.ReservationMateriel;
// import mg.itu.service.CsvService;
// import mg.itu.service.RoleService;
// import mg.itu.service.UtilisateurService;
// import mg.itu.service.EspaceTravailService;
// import mg.itu.service.OptionService;
// import mg.itu.service.PaiementService;
// import mg.itu.service.ReservationService;
// import mg.itu.service.ReservationMaterielService;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize;
// import org.springframework.web.bind.annotation.*;
// import org.springframework.web.multipart.MultipartFile;
// import site.easy.to.build.crm.service.csv.CsvService;

// import java.nio.file.Files;
// import java.nio.file.Path;
// import java.nio.file.StandardCopyOption;
// import java.util.List;
// import java.util.ArrayList;

// @RestController
// @RequestMapping("/back-office/api/importCsv")
// @CrossOrigin(origins = "*")
// public class CsvController {
//     @Autowired
//     private EspaceTravailService espaceTravailService;
    
//     @Autowired
//     private OptionService optionService;
    
//     @Autowired
//     private PaiementService paiementService;
    
//     @Autowired
//     private ReservationService reservationService;

//     @Autowired
//     private ReservationMaterielService reservationMaterielService;

//     @Autowired
//     private CsvService csvService;

//     @Autowired
//     private UtilisateurService utilisateurService;

//     @Autowired
//     private RoleService roleService;

//     @PreAuthorize("hasRole('ADMIN')")
    // @PostMapping("/espace")
    // public ResponseEntity<?> importEspace(@RequestParam("file") MultipartFile file) {
    //     try {
    //         Path tempFile = Files.createTempFile("csv_upload_", file.getOriginalFilename());
    //         Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
            
    //         List<EspaceDto> importedData = csvService.importCsv(tempFile.toString(),EspaceDto.class);
    //         List<EspaceTravail> savedEspaces = new ArrayList<>();
            
    //         for (EspaceDto espace : importedData) {
    //             EspaceTravail newEspace = new EspaceTravail(espace.getNom(), espace.getPrixHeure());
    //             savedEspaces.add(espaceTravailService.saveEspaceTravail(newEspace));
    //         }
            
    //         Files.deleteIfExists(tempFile);
    //         return ResponseEntity.ok(savedEspaces);
    //     } catch (Exception e) {
    //         return ResponseEntity.internalServerError().body("Erreur lors de l'import : " + e.getMessage());
    //     }
    // }

//     @PreAuthorize("hasRole('ADMIN')")
//     @PostMapping("/option")
//     public ResponseEntity<?> importOption(@RequestParam("file") MultipartFile file) {
//         try {
//             Path tempFile = Files.createTempFile("csv_upload_", file.getOriginalFilename());
//             Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
            
//             List<OptionDto> importedData = csvService.importCsv(tempFile.toString(),OptionDto.class);
//             List<Option> savedOptions = new ArrayList<>();
            
//             for (OptionDto optionDto : importedData) {
//                 Option newOption = new Option(optionDto.getCode(),optionDto.getOption(), optionDto.getPrix());
//                 savedOptions.add(optionService.saveOption(newOption));
//             }
            
//             Files.deleteIfExists(tempFile);
//             return ResponseEntity.ok(savedOptions);
//         } catch (Exception e) {
//             return ResponseEntity.internalServerError().body("Erreur lors de l'import : " + e.getMessage());
//         }
//     }

//     @PreAuthorize("hasRole('ADMIN')")
//     @PostMapping("/paiement")
//     public ResponseEntity<?> importPaiement(@RequestParam("file") MultipartFile file) {
//         try {
//             Path tempFile = Files.createTempFile("csv_upload_", file.getOriginalFilename());
//             Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
            
//             List<PaiementDto> importedData = csvService.importCsv(tempFile.toString(),PaiementDto.class);
//             List<Paiement> savedPaiements = new ArrayList<>();
            
//             for (PaiementDto paiementDto : importedData) {
//                 Reservation reservation = reservationService.findByIdReservation(paiementDto.getRef());
//                 Paiement newPaiement = new Paiement(paiementDto.getRef(), paiementDto.getDate(),reservation); 
//                 savedPaiements.add(paiementService.savePaiement(newPaiement));
//             }
            
//             Files.deleteIfExists(tempFile);
//             return ResponseEntity.ok(savedPaiements);
//         } catch (Exception e) {
//             return ResponseEntity.internalServerError().body("Erreur lors de l'import : " + e.getMessage());
//         }
//     }

//     @PreAuthorize("hasRole('ADMIN')")
//     @PostMapping("/reservation")
//     public ResponseEntity<?> importReservation(@RequestParam("file") MultipartFile file) {
//         try {
//             Path tempFile = Files.createTempFile("csv_upload_", file.getOriginalFilename());
//             Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
            
//             List<ReservationDto> importedData = csvService.importCsv(tempFile.toString(),ReservationDto.class);
//             List<Reservation> savedReservations = new ArrayList<>();
//             for (ReservationDto reservationDto : importedData) {
//                 utilisateurService.saveUtilisateur(new Utilisateur(
//                     reservationDto.getClient().toString(),
//                     reservationDto.getClient().toString(),
//                     null , null , null , null ,
//                     roleService.findByLibelle("user")
//                 ));
//             }
            
//             for (ReservationDto reservationDto : importedData) {
//                 Reservation newReservation = new Reservation(
//                     reservationDto.getRef(),
//                     reservationDto.getDate(),
//                     reservationDto.getHeureDebut(),
//                     null, 
//                     null, 
//                     reservationDto.getDuree(),
//                     null, 
//                     null, 
//                     new Utilisateur(reservationDto.getClient().toString(),reservationDto.getClient().toString()),
//                     espaceTravailService.findByLibelle(reservationDto.getEspace())
//                 );

//                 savedReservations.add(reservationService.saveReservation(newReservation));

//                 for(String option : reservationDto.getOption()){
//                     Reservation reservation = reservationService.findByIdReservation(reservationDto.getRef());
//                     ReservationMateriel reservationMateriel = new ReservationMateriel(reservation,optionService.findByIdOption(option));
//                     reservationMaterielService.saveReservationMateriel(reservationMateriel);
//                 }
//             }
            
//             Files.deleteIfExists(tempFile);
//             return ResponseEntity.ok(savedReservations);
//         } catch (Exception e) {
//             return ResponseEntity.internalServerError().body("Erreur lors de l'import : " + e.getMessage());
//         }
//     }
// }