package com.example.demo.config;

import com.example.demo.Models.*;
import com.example.demo.Repositories.*;
import com.example.demo.Services.CommunityService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Component
@Profile("demo")
public class DataSeeder implements CommandLineRunner {

    @PersistenceContext
    private EntityManager em;

    private final CommunityService community;
    private final com.example.demo.Services.StudioService studioService;
    private final com.example.demo.Services.JobService jobService;
    private final com.example.demo.Services.StateMovementService movementService;
    private final com.example.demo.Services.GroupJoinRequestService joinRequestService;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final GroupRepository groupRepository;
    private final CommentRepository commentRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(CommunityService community,
                      com.example.demo.Services.StudioService studioService,
                      com.example.demo.Services.JobService jobService,
                      com.example.demo.Services.StateMovementService movementService,
                      com.example.demo.Services.GroupJoinRequestService joinRequestService,
                      UserRepository userRepository,
                      PostRepository postRepository,
                      GroupRepository groupRepository,
                      CommentRepository commentRepository,
                      PasswordEncoder passwordEncoder) {
        this.community = community;
        this.studioService = studioService;
        this.jobService = jobService;
        this.movementService = movementService;
        this.joinRequestService = joinRequestService;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.groupRepository = groupRepository;
        this.commentRepository = commentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Cada bloque se carga una sola vez; nunca sobrescribe cuentas, bandas o reservas editadas.
        if (userRepository.count() < 20) seedCommunity();
        if (em.createQuery("select count(s) from Studio s", Long.class).getSingleResult() == 0) seedStudios();
        if (em.createQuery("select count(j) from JobPosting j", Long.class).getSingleResult() == 0) seedJobs();
        if (em.createQuery("select count(p) from Profile p where p.avatarUrl is not null", Long.class).getSingleResult() == 0
                || em.createQuery("select count(g) from Group g where g.avatarUrl is not null", Long.class).getSingleResult() == 0) {
            seedDefaultAvatars();
        }
        if (em.createQuery("select count(m) from StateMovement m", Long.class).getSingleResult() == 0) {
            seedSampleRequestsAndBackfillMovements();
        }
    }

    private void seedSampleRequestsAndBackfillMovements() {
        // 1. Backfill de avisos existentes
        for (JobPosting j : em.createQuery("select j from JobPosting j join fetch j.author", JobPosting.class).getResultList()) {
            movementService.record(MovementEntityType.JOB_POSTING, j.getId(), j.getTitle(),
                    null, "OPEN", j.getAuthor(), "Aviso publicado en la bolsa de trabajo");
            if (!j.isOpen()) {
                movementService.record(MovementEntityType.JOB_POSTING, j.getId(), j.getTitle(),
                        "OPEN", "CLOSED", j.getAuthor(), "Aviso cerrado por el autor");
            }
        }
        // 2. Backfill de postulaciones existentes
        for (JobApplication a : em.createQuery("select a from JobApplication a join fetch a.applicant join fetch a.job j join fetch j.author", JobApplication.class).getResultList()) {
            String label = a.getApplicant().getUsername() + " → " + a.getJob().getTitle();
            movementService.record(MovementEntityType.JOB_APPLICATION, a.getId(), label,
                    null, ApplicationStatus.PENDING.name(), a.getApplicant(),
                    a.getMessage() != null ? "Postulación enviada: " + a.getMessage() : "Postulación enviada");
            if (a.getStatus() != ApplicationStatus.PENDING) {
                movementService.record(MovementEntityType.JOB_APPLICATION, a.getId(), label,
                        ApplicationStatus.PENDING.name(), a.getStatus().name(), a.getJob().getAuthor(),
                        a.getStatus() == ApplicationStatus.ACCEPTED
                                ? "Postulación aceptada por el autor del aviso"
                                : "Postulación rechazada por el autor del aviso");
            }
        }
        // 3. Backfill de reservas existentes
        for (StudioBooking b : em.createQuery("select b from StudioBooking b join fetch b.user join fetch b.studio s join fetch s.owner", StudioBooking.class).getResultList()) {
            String label = b.getUser().getUsername() + " → " + b.getStudio().getName();
            movementService.record(MovementEntityType.STUDIO_BOOKING, b.getId(), label,
                    null, BookingStatus.CONFIRMED.name(), b.getUser(), "Reserva confirmada");
            if (b.getStatus() == BookingStatus.CANCELLED) {
                movementService.record(MovementEntityType.STUDIO_BOOKING, b.getId(), label,
                        BookingStatus.CONFIRMED.name(), BookingStatus.CANCELLED.name(), b.getUser(), "Reserva cancelada");
            }
        }
        // 4. Crear solicitudes de ingreso a bandas de ejemplo con distintos estados (PENDING, REJECTED, CANCELLED)
        Group nebula = groupRepository.findByNameAndDeletedFalse("Nebula Waves").orElse(null);
        Group errantes = groupRepository.findByNameAndDeletedFalse("Los Errantes").orElse(null);
        User gladito = demoUser("gladito");
        User nico = demoUser("nicofontana");
        User elena = demoUser("elenavarela");
        User rodrigo = demoUser("rodrigopaz");

        if (nebula != null && gladito != null && nico != null && elena != null && rodrigo != null) {
            try {
                // Solicitud 1: Nico solicita unirse a Nebula Waves -> Queda PENDING para que Gladito la vea en /groups/requests
                var dto1 = new com.example.demo.Dtos.Groups.GroupJoinRequestCreateDto();
                dto1.setGroupId(nebula.getId());
                dto1.setMessage("Tengo sintetizadores modulares Eurorack para sumar secuencias en vivo.");
                joinRequestService.submitRequest(nico.getId(), dto1);
            } catch (Exception ignored) {}

            try {
                // Solicitud 2: Rodrigo solicita unirse a Nebula Waves y luego Gladito la RECHAZA (INICIAL -> PENDING -> REJECTED)
                var dto2 = new com.example.demo.Dtos.Groups.GroupJoinRequestCreateDto();
                dto2.setGroupId(nebula.getId());
                dto2.setMessage("¿Buscan segundo bajista para las fechas de este mes?");
                joinRequestService.submitRequest(rodrigo.getId(), dto2);
                joinRequestService.getPendingRequestsByRequester(rodrigo.getId()).stream()
                        .filter(r -> r.getGroupId().equals(nebula.getId()))
                        .findFirst()
                        .ifPresent(r -> joinRequestService.rejectRequest(r.getId(), gladito.getId()));
            } catch (Exception ignored) {}

            try {
                // Solicitud 3: Elena solicita unirse a Nebula Waves y luego ella misma la CANCELA (INICIAL -> PENDING -> CANCELLED)
                var dto3 = new com.example.demo.Dtos.Groups.GroupJoinRequestCreateDto();
                dto3.setGroupId(nebula.getId());
                dto3.setMessage("Me encantaría sumar texturas de arpa eléctrica.");
                joinRequestService.submitRequest(elena.getId(), dto3);
                joinRequestService.getPendingRequestsByRequester(elena.getId()).stream()
                        .filter(r -> r.getGroupId().equals(nebula.getId()))
                        .findFirst()
                        .ifPresent(r -> joinRequestService.cancelRequest(r.getId(), elena.getId()));
            } catch (Exception ignored) {}
        }

        if (errantes != null && gladito != null) {
            try {
                // Solicitud 4: Gladito solicita unirse a Los Errantes -> Queda PENDING en "Mis solicitudes enviadas" para poder probar Cancelar en vivo
                var dto4 = new com.example.demo.Dtos.Groups.GroupJoinRequestCreateDto();
                dto4.setGroupId(errantes.getId());
                dto4.setMessage("Puedo sumar sintetizadores distorsionados y efectos para el vivo.");
                joinRequestService.submitRequest(gladito.getId(), dto4);
            } catch (Exception ignored) {}
        }
    }

    private void seedDefaultAvatars() {
        Map<String, String> userAvatars = Map.ofEntries(
                Map.entry("gladito", "/images/avatars/user-gladito.svg"),
                Map.entry("pepito", "/images/avatars/user-pepito.svg"),
                Map.entry("camitorres", "/images/avatars/user-camitorres.svg"),
                Map.entry("mateosilv", "/images/avatars/user-mateosilv.svg"),
                Map.entry("luciamorales", "/images/avatars/user-luciamorales.svg"),
                Map.entry("francoherrera", "/images/avatars/user-francoherrera.svg"),
                Map.entry("julietamusic", "/images/avatars/user-julietamusic.svg"),
                Map.entry("claramendez", "/images/avatars/user-claramendez.svg"),
                Map.entry("santibenitez", "/images/avatars/user-santibenitez.svg"),
                Map.entry("nachopaez", "/images/avatars/user-nachopaez.svg"),
                Map.entry("sofiarossi", "/images/avatars/user-sofiarossi.svg"),
                Map.entry("rodrigopaz", "/images/avatars/user-rodrigopaz.svg"),
                Map.entry("tomasduarte", "/images/avatars/user-tomasduarte.svg"),
                Map.entry("florenciac", "/images/avatars/user-florenciac.svg"),
                Map.entry("valenvega", "/images/avatars/user-valenvega.svg"),
                Map.entry("martinaq", "/images/avatars/user-martinaq.svg"),
                Map.entry("gonzaruiz", "/images/avatars/user-gonzaruiz.svg"),
                Map.entry("brunocast", "/images/avatars/user-brunocast.svg"),
                Map.entry("elenavarela", "/images/avatars/user-elenavarela.svg"),
                Map.entry("nicofontana", "/images/avatars/user-nicofontana.svg")
        );
        userAvatars.forEach((uname, url) -> userRepository.findByUsernameAndDeletedFalse(uname).ifPresent(u -> {
            if (u.getProfile() != null && (u.getProfile().getAvatarUrl() == null || u.getProfile().getAvatarUrl().isBlank())) {
                u.getProfile().setAvatarUrl(url);
            }
        }));

        Map<String, String> bandAvatars = Map.of(
                "Nebula Waves", "/images/avatars/band-nebula.svg",
                "Ecos del Sur", "/images/avatars/band-ecos.svg",
                "Jazz Noir Collective", "/images/avatars/band-jazznoir.svg",
                "Los Errantes", "/images/avatars/band-errantes.svg",
                "Ritmo Subterráneo", "/images/avatars/band-ritmo.svg"
        );
        bandAvatars.forEach((bname, url) -> groupRepository.findByNameAndDeletedFalse(bname).ifPresent(g -> {
            if (g.getAvatarUrl() == null || g.getAvatarUrl().isBlank()) {
                g.setAvatarUrl(url);
            }
        }));
    }

    private User demoUser(String username) {
        return userRepository.findByUsernameAndDeletedFalse(username).orElse(null);
    }

    /** Salas de ensayo y estudios con algunas reservas futuras. */
    private void seedStudios() {
        User rodrigo = demoUser("rodrigopaz"), flor = demoUser("florenciac"), nico = demoUser("nicofontana"),
             elena = demoUser("elenavarela"), tomas = demoUser("tomasduarte");
        if (rodrigo == null || flor == null || nico == null || elena == null || tomas == null) return;

        Studio dorada = studioService.create(rodrigo.getId(), "La Sala Dorada", StudioType.RECORDING,
                "Estudio de grabación con sala de toma amplia y control room tratado acústicamente. Ideal para discos de banda completa.",
                "Palermo, CABA", "Piano de cola · Micrófonos vintage · Pro Tools HD · Ingeniero incluido", 45000);
        Studio subsuelo = studioService.create(flor.getId(), "Subsuelo Groove", StudioType.REHEARSAL,
                "Sala de ensayo insonorizada para bandas de rock, funk y punk. Backline listo para enchufar y tocar.",
                "Almagro, CABA", "Batería completa · Amplis de bajo y guitarra · PA 2000W · Acceso 24/7", 9000);
        Studio orbita = studioService.create(nico.getId(), "Estudio Órbita", StudioType.RECORDING,
                "Estudio orientado a música electrónica y producción con sintetizadores analógicos.",
                "Villa Crespo, CABA", "Sintes modulares · Monitores de campo cercano · Consola analógica", 30000);
        Studio cabina = studioService.create(elena.getId(), "Cabina Eco", StudioType.VOCAL_BOOTH,
                "Cabina vocal tratada para voces, locuciones y podcasts.",
                "Caballito, CABA", "Cabina tratada · Micrófono de condensador de gran diafragma · Interfaz de audio", 12000);
        studioService.create(tomas.getId(), "Galpón Errante", StudioType.REHEARSAL,
                "Galpón amplio para ensayos de hasta seis músicos y preproducción de shows.",
                "Avellaneda, Buenos Aires", "Sala para 6 músicos · Backline completo · Estacionamiento", 7500);
        em.flush();

        java.time.LocalDate today = java.time.LocalDate.now();
        safeBook("gladito", subsuelo, today.plusDays(1).atTime(20, 0), 2);
        safeBook("luciamorales", cabina, today.plusDays(2).atTime(16, 0), 1);
        safeBook("santibenitez", dorada, today.plusDays(3).atTime(14, 0), 4);
        safeBook("martinaq", orbita, today.plusDays(5).atTime(19, 0), 3);
        safeBook("camitorres", subsuelo, today.plusDays(1).atTime(17, 0), 2);
    }

    private void safeBook(String username, Studio studio, java.time.LocalDateTime start, int hours) {
        User u = demoUser(username);
        if (u == null) return;
        try {
            studioService.book(u.getId(), studio.getId(), start.toString(), hours);
        } catch (Exception ignored) {}
    }

    /** Avisos de la bolsa de trabajo y postulaciones de ejemplo. */
    private void seedJobs() {
        User lucia = demoUser("luciamorales"), santi = demoUser("santibenitez"), martina = demoUser("martinaq"),
             tomas = demoUser("tomasduarte"), gladito = demoUser("gladito"), gonza = demoUser("gonzaruiz");
        if (lucia == null || santi == null || martina == null || tomas == null || gladito == null || gonza == null) return;

        JobPosting cello = jobService.create(lucia.getId(), "Buscamos cellista para grabación de EP", JobType.SESSION,
                "Ecos del Sur graba un EP de cinco canciones de indie folk. Buscamos cellista para dos jornadas de estudio. Partituras disponibles.",
                "Cello", "Palermo, CABA", false, "$ 25.000 por sesión");
        JobPosting piano = jobService.create(santi.getId(), "Pianista para ciclo de jazz los jueves", JobType.LIVE_GIG,
                "Ciclo semanal de jazz en un bar de San Telmo. Repertorio de standards y algo de neo-soul. Se valora lectura e improvisación.",
                "Piano", "San Telmo, CABA", false, "$ 40.000 por fecha");
        JobPosting mezcla = jobService.create(martina.getId(), "Ingeniero/a de mezcla para EP de beats", JobType.POST_PRODUCTION,
                "Seis beats instrumentales con stems listos. Buscamos mezcla cálida con estética boom bap y entrega en tres semanas.",
                "Mezcla", "Remoto", true, "$ 18.000 por tema");
        JobPosting bajista = jobService.create(tomas.getId(), "Bajista para Los Errantes", JobType.BAND_MEMBER,
                "Banda de garage rock busca bajista estable. Ensayamos dos veces por semana en Avellaneda y tenemos fechas confirmadas.",
                "Bajo", "Avellaneda, Buenos Aires", false, "A convenir");
        JobPosting clases = jobService.create(gladito.getId(), "Busco profe de síntesis modular", JobType.TEACHING,
                "Quiero aprender patcheo en Eurorack: osciladores, envolventes y secuenciación. Clases semanales.",
                "Sintetizador", "Remoto", true, "$ 15.000 por clase");
        jobService.create(gonza.getId(), "Percusionista para boda", JobType.LIVE_GIG,
                "Show de salsa y cumbia para un casamiento. Set de dos horas con banda completa.",
                "Percusión", "Quilmes, Buenos Aires", false, "$ 60.000 total");
        em.flush();

        safeApply("claramendez", cello, "Toqué en varios discos de folk; puedo ir las dos jornadas.");
        safeApply("rodrigopaz", mezcla, "Mezclo en analógico con cinta, te paso trabajos anteriores.");
        safeApply("pepito", bajista, "Me encanta el garage, tengo equipo propio y disponibilidad.");
        safeApply("francoherrera", bajista, "Toco contrabajo y bajo eléctrico, puedo sumarme a los ensayos.");
        safeApply("nicofontana", clases, "Doy clases de modular hace tres años, te armo un programa a medida.");
        safeApply("luciamorales", piano, "Puedo cubrir algunas fechas en piano.");
        // Una postulación ya respondida para mostrar el flujo completo.
        User clara = demoUser("claramendez");
        if (clara != null) jobService.myApplication(cello.getId(), clara.getId())
                .ifPresent(a -> jobService.decide(lucia.getId(), a.getId(), true));
    }

    private void safeApply(String username, JobPosting job, String message) {
        User u = demoUser(username);
        if (u == null) return;
        try {
            jobService.apply(u.getId(), job.getId(), message);
        } catch (Exception ignored) {}
    }

    private void seedCommunity() {
        // --- 1. PERSONAS (20 Músicos con identidad y bios artísticas) ---
        User gladito = ensureUser("Agustin", "Navarro", LocalDate.of(1995, 5, 15), "gladito", "gladito@example.test",
                "Productor de sonidos atmosféricos. Sintetizadores analógicos, ambient y beats espaciales.");

        User pepito = ensureUser("Pepito", "Gomez", LocalDate.of(1998, 7, 8), "pepito", "pepito@example.test",
                "Bajista con ganas de compartir grooves funk, líneas melódicas y neo-soul.");

        User cami = ensureUser("Camila", "Torres", LocalDate.of(1999, 3, 22), "camitorres", "camila@example.test",
                "Baterista sesionista. Amante de compases irregulares, math rock y jazz fusion.");

        User mateo = ensureUser("Mateo", "Silveira", LocalDate.of(1997, 11, 14), "mateosilv", "mateo@example.test",
                "Guitarrista experimental. Amante de las cámaras de eco, fuzz analógico y texturas espaciales.");

        User lucia = ensureUser("Lucia", "Morales", LocalDate.of(1996, 9, 30), "luciamorales", "lucia@example.test",
                "Cantautora y pianista. Componiendo canciones intimistas de indie folk y formato acústico.");

        User franco = ensureUser("Franco", "Herrera", LocalDate.of(1994, 1, 18), "francoherrera", "franco@example.test",
                "Contrabajista clásico y bajo fretless. Arreglador musical y productor independiente.");

        User julieta = ensureUser("Julieta", "Albarracin", LocalDate.of(2000, 6, 12), "julietamusic", "julieta@example.test",
                "Voz y guitarra acústica. Melodías nostálgicas para escuchar al atardecer.");

        User clara = ensureUser("Clara", "Mendez", LocalDate.of(1997, 4, 5), "claramendez", "clara@example.test",
                "Cellista de sesión. Explorando la combinación de cuerdas frotadas con pedales de delay y reverb.");

        User santi = ensureUser("Santiago", "Benitez", LocalDate.of(1993, 12, 3), "santibenitez", "santi@example.test",
                "Saxofón tenor y alto. Sesionista de bebop, hard-bop y neo-soul en Buenos Aires.");

        User nacho = ensureUser("Ignacio", "Paez", LocalDate.of(1995, 8, 27), "nachopaez", "nacho@example.test",
                "Trompeta con sordina. Colaboraciones en vivo, jazz nocturno y arreglos de vientos.");

        User sofia = ensureUser("Sofia", "Rossi", LocalDate.of(1998, 10, 19), "sofiarossi", "sofia@example.test",
                "Flauta traversa y quena. Fusionando folclore andino con armonías de jazz modal.");

        User rodrigo = ensureUser("Rodrigo", "Paz", LocalDate.of(1992, 2, 14), "rodrigopaz", "rodrigo@example.test",
                "Bajo fretless y producción de mezcla. Buscando siempre la calidez analógica del sonido de cinta.");

        User tomas = ensureUser("Tomas", "Duarte", LocalDate.of(1996, 5, 9), "tomasduarte", "tomas@example.test",
                "Guitarra eléctrica y voz. Riffs valvulares, garage rock crudo y actitud post-punk.");

        User flor = ensureUser("Florencia", "Carrizo", LocalDate.of(1999, 7, 25), "florenciac", "flor@example.test",
                "Baterista de garage rock y punk. Golpes directos, potencia, tempo firme y mucho nervio.");

        User valen = ensureUser("Valentina", "Vega", LocalDate.of(2001, 2, 11), "valenvega", "valen@example.test",
                "Violín eléctrico con distorsión. Experimentación sonora, noise ambiental y música incidental.");

        User martina = ensureUser("Martina", "Quiroga", LocalDate.of(1998, 4, 16), "martinaq", "martina@example.test",
                "Beatmaker y sampleadora en MPC. Hip-hop instrumental noventero, boom bap y neo-soul.");

        User gonza = ensureUser("Gonzalo", "Ruiz", LocalDate.of(1994, 9, 8), "gonzaruiz", "gonza@example.test",
                "Percusionista latino: congas, timbales y bongó. Sabor afrocubano, salsa y funk con clave.");

        User bruno = ensureUser("Bruno", "Castellani", LocalDate.of(1995, 12, 29), "brunocast", "bruno@example.test",
                "Guitarrista clásico y flamenco. Búsqueda de armonías modernas sobre madera criolla.");

        User elena = ensureUser("Elena", "Varela", LocalDate.of(1997, 8, 1), "elenavarela", "elena@example.test",
                "Arpa clásica y texturas vocales etéreas. Música para cine, paisajes celtas y videojuegos.");

        User nico = ensureUser("Nicolas", "Fontana", LocalDate.of(1993, 6, 20), "nicofontana", "nico@example.test",
                "Sintetizadores modulares analógicos y cajas de ritmo. Ambient espacial y synthwave.");

        em.flush();

        // --- 2. BANDAS Y ROLES DE INTEGRANTES ---
        // Banda 1: Nebula Waves
        Group nebula = ensureBand("Nebula Waves", "Colectivo de sintetizadores analógicos, ritmos espaciales y texturas synthwave.", gladito);
        ensureMember(nebula, gladito, gladito, "Sintetizador", true, true);
        ensureMember(nebula, gladito, pepito, "Bajo eléctrico", true, false);
        ensureMember(nebula, gladito, cami, "Batería", true, false);
        ensureMember(nebula, gladito, mateo, "Guitarra eléctrica & FX", false, false);

        // Banda 2: Ecos del Sur
        Group ecos = ensureBand("Ecos del Sur", "Indie folk y música acústica de raíz con armonías vocales y cuerdas.", lucia);
        ensureMember(ecos, lucia, lucia, "Voz principal & Piano", true, true);
        ensureMember(ecos, lucia, franco, "Contrabajo", true, false);
        ensureMember(ecos, lucia, julieta, "Guitarra acústica & Coros", true, false);
        ensureMember(ecos, lucia, clara, "Cello", false, false);

        // Banda 3: Jazz Noir Collective
        Group jazzNoir = ensureBand("Jazz Noir Collective", "Ensamble de vientos y jazz nocturno. Sesiones de improvisación y neo-soul.", santi);
        ensureMember(jazzNoir, santi, santi, "Saxofón tenor", true, true);
        ensureMember(jazzNoir, santi, nacho, "Trompeta con sordina", true, false);
        ensureMember(jazzNoir, santi, sofia, "Flauta traversa", false, false);
        ensureMember(jazzNoir, santi, rodrigo, "Bajo fretless", false, false);

        // Banda 4: Los Errantes
        Group errantes = ensureBand("Los Errantes", "Cuarteto de garage rock, post-punk y sonido valvular crudo en vivo.", tomas);
        ensureMember(errantes, tomas, tomas, "Guitarra eléctrica & Voz", true, true);
        ensureMember(errantes, tomas, flor, "Batería acústica", true, false);
        ensureMember(errantes, tomas, valen, "Violín distorsionado", false, false);

        // Banda 5: Ritmo Subterráneo
        Group ritmo = ensureBand("Ritmo Subterráneo", "Fusión de beats instrumentales, percusión afrocubana y guitarras flamencas.", martina);
        ensureMember(ritmo, martina, martina, "MPC Sampler & Beats", true, true);
        ensureMember(ritmo, martina, gonza, "Congas & Timbales", true, false);
        ensureMember(ritmo, martina, bruno, "Guitarra flamenca", false, false);
        ensureMember(ritmo, martina, elena, "Arpa & Texturas", false, false);
        ensureMember(ritmo, martina, nico, "Sintes modulares", false, false);

        em.flush();

        // --- 3. PUBLICACIONES EN EL FEED (Con audio y etiquetas) ---
        Post p1 = ensurePost("Nebula Jam Session", "Layers atmosféricos con sintetizadores analógicos y batería suave.", "/demo-tone.wav", 4, gladito, nebula, List.of("synthwave", "ambient", "electronica"));
        Post p2 = ensurePost("Midnight Groove", "Groove funk con toques de neo-soul, ideal para improvisar sobre él.", "/demo-tone.wav", 4, pepito, null, List.of("funk", "bajo", "groove"));
        Post p3 = ensurePost("Viento en las Sierras", "Melodía acústica grabada en una toma directa junto al fogón.", "/demo-tone.wav", 4, lucia, ecos, List.of("indie", "folk", "acustico"));
        Post p4 = ensurePost("Blue Midnight Session", "Improvisación modal en escala menor para noches de lluvia.", "/demo-tone.wav", 4, santi, jazzNoir, List.of("jazz", "saxo", "neosoul"));
        Post p5 = ensurePost("Furia Valvular en Vivo", "Ensayo crudo a dos guitarras con distorsión analógica al límite.", "/demo-tone.wav", 4, tomas, errantes, List.of("rock", "garage", "punk"));
        Post p6 = ensurePost("Low-End Theory Beats", "Sampleando vinilos viejos de jazz con un golpe de bombo noventero.", "/demo-tone.wav", 4, martina, ritmo, List.of("lofi", "beats", "sampling"));
        Post p7 = ensurePost("Patrones en 7/8", "Exploración rítmica inspirada en el jazz fusión y compases impares.", "/demo-tone.wav", 4, cami, null, List.of("bateria", "groove", "fusion"));
        Post p8 = ensurePost("Nostalgia en Seis Cuerdas", "Pequeña maqueta acústica buscando arreglos de cuerdas y coros.", "/demo-tone.wav", 4, julieta, null, List.of("acustico", "voz", "cancion"));
        Post p9 = ensurePost("Nocturno para Cello y Delay", "Texturas lentas de arco con repeticiones oscuras de pedal.", "/demo-tone.wav", 4, clara, null, List.of("cello", "experimental", "ambiental"));
        Post p10 = ensurePost("Exploración Modular #4", "Secuencias polirítmicas generadas con osciladores analógicos.", "/demo-tone.wav", 4, nico, null, List.of("modular", "sintetizador", "analogico"));
        Post p11 = ensurePost("Fantasía Celta", "Arpegios de arpa grabados en acústica natural.", "/demo-tone.wav", 4, elena, null, List.of("arpa", "cinematica", "etereo"));
        Post p12 = ensurePost("Descarga de Timbales", "Rumba callejera y clave 3-2 acelerada con percusión pura.", "/demo-tone.wav", 4, gonza, null, List.of("percusion", "latino", "salsa"));

        em.flush();

        // --- 4. COMENTARIOS ENTRE MÚSICOS ---
        ensureComment(p1, pepito, "¡Suena increíble! Podríamos agregar un bajo sutil al segundo verso.");
        ensureComment(p1, cami, "Esa base de sintetizador con la batería en 7/8 quedaría tremenda.");
        ensureComment(p1, nico, "¿Qué filtro analógico usaste para el pad de fondo? Tiene un timbre hermoso.");

        ensureComment(p2, gladito, "Tremendo groove Pepito, le podemos sumar un pad atmosférico.");
        ensureComment(p2, mateo, "Ese bajo pide a gritos unos arreglos de guitarra con wah-wah.");

        ensureComment(p3, franco, "Las armonías vocales se lucen con el contrabajo acústico de fondo.");
        ensureComment(p3, clara, "Me encantó la melodía Lucía, contá conmigo para sumarle unos cellos.");

        ensureComment(p4, nacho, "La próxima vuelta le metemos la sordina en la trompeta al puente.");
        ensureComment(p4, sofia, "Esa escala menor invita a sumar unas frases de flauta en contrapunto.");

        ensureComment(p5, flor, "¡Esa fuerza no se negocia! En el próximo ensayo le metemos más velocidad.");
        ensureComment(p5, valen, "La distorsión del violín encajó perfecto con ese riff valvular.");

        ensureComment(p6, gonza, "¡Qué sabroso! Le meto unas congas bien afinadas arriba y explota.");
        ensureComment(p6, santi, "Ese beat pide un saxo tenor bien grave y con aire.");

        // --- 5. REACCIONES (Likes en el Feed) ---
        safeReact(pepito, p1, 1);
        safeReact(cami, p1, 1);
        safeReact(mateo, p1, 1);
        safeReact(nico, p1, 1);

        safeReact(gladito, p2, 1);
        safeReact(mateo, p2, 1);
        safeReact(cami, p2, 1);

        safeReact(franco, p3, 1);
        safeReact(julieta, p3, 1);
        safeReact(clara, p3, 1);
        safeReact(elena, p3, 1);

        safeReact(nacho, p4, 1);
        safeReact(sofia, p4, 1);
        safeReact(rodrigo, p4, 1);

        safeReact(flor, p5, 1);
        safeReact(valen, p5, 1);

        safeReact(gonza, p6, 1);
        safeReact(bruno, p6, 1);
        safeReact(santi, p6, 1);

        // --- 6. CALIFICACIONES ENTRE MÚSICOS (Ratings 1-5 estrellas) ---
        safeRate(pepito, gladito, 5);
        safeRate(cami, gladito, 5);
        safeRate(mateo, gladito, 5);
        safeRate(nico, gladito, 5);

        safeRate(gladito, pepito, 5);
        safeRate(cami, pepito, 5);
        safeRate(mateo, pepito, 4);

        safeRate(lucia, franco, 5);
        safeRate(julieta, franco, 5);
        safeRate(franco, lucia, 5);
        safeRate(clara, lucia, 5);

        safeRate(santi, nacho, 5);
        safeRate(nacho, santi, 5);
        safeRate(sofia, santi, 5);

        safeRate(tomas, flor, 5);
        safeRate(flor, tomas, 5);

        safeRate(martina, gonza, 5);
        safeRate(gonza, martina, 5);
        safeRate(bruno, martina, 5);
        safeRate(elena, clara, 5);
    }

    private User ensureUser(String firstName, String lastName, LocalDate birthDate, String username, String email, String bio) {
        User user = userRepository.findByUsernameAndDeletedFalse(username).orElseGet(() -> {
            User u = new User(firstName, lastName, birthDate, username, email, passwordEncoder.encode("Harmony123"));
            return userRepository.save(u);
        });
        if (user.getProfile() != null && (user.getProfile().getBio() == null || user.getProfile().getBio().isBlank())) {
            user.getProfile().setBio(bio);
        }
        return user;
    }

    private Group ensureBand(String name, String description, User owner) {
        return groupRepository.findByNameAndDeletedFalse(name).orElseGet(() -> {
            Group band = new Group(name, description, owner);
            return groupRepository.save(band);
        });
    }

    private void ensureMember(Group band, User owner, User member, String instrument, boolean publish, boolean approve) {
        if (!band.hasMember(member)) {
            band.addMember(member);
            groupRepository.save(band);
            em.flush();
        }
        try {
            community.member(band.getId(), owner.getId(), member.getId(), instrument, publish, approve);
        } catch (Exception ignored) {}
    }

    private Tag getOrCreateTag(String name) {
        String val = name.toLowerCase(Locale.ROOT).trim();
        List<Tag> found = em.createQuery("from Tag t where t.value=:val", Tag.class)
                .setParameter("val", val)
                .getResultList();
        if (!found.isEmpty()) return found.get(0);
        Tag t = new Tag(val);
        em.persist(t);
        return t;
    }

    private Post ensurePost(String title, String description, String audioUrl, Integer duration, User author, Group band, List<String> tagNames) {
        Post post = postRepository.findAllByAuthorAndDeletedFalse(author)
                .stream()
                .filter(p -> title.equals(p.getTitle()))
                .findFirst()
                .orElseGet(() -> {
                    Post newPost = new Post(title, description, Visibility.PUBLIC, audioUrl, duration, author);
                    if (band != null) newPost.setBand(band);
                    author.getProfile().addPost(newPost);
                    return postRepository.save(newPost);
                });

        if (band != null && post.getBand() == null) {
            post.setBand(band);
        }
        if (tagNames != null) {
            for (String t : tagNames) {
                Tag tag = getOrCreateTag(t);
                post.addTag(tag);
            }
        }
        return post;
    }

    private void ensureComment(Post post, User author, String content) {
        boolean exists = commentRepository.findAllByPostAndDeletedFalseOrderByCreatedAtAsc(post)
                .stream()
                .anyMatch(c -> author.equals(c.getAuthor()) && content.equals(c.getContent()));
        if (!exists) {
            Comment comment = new Comment(post, author, content);
            commentRepository.save(comment);
        }
    }

    private void safeRate(User actor, User target, int score) {
        try {
            community.rate(actor.getId(), target.getId(), score);
        } catch (Exception ignored) {}
    }

    private void safeReact(User actor, Post post, int value) {
        try {
            community.react(actor.getId(), post.getId(), value);
        } catch (Exception ignored) {}
    }
}
