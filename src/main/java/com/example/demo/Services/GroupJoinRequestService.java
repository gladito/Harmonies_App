package com.example.demo.Services;

import com.example.demo.Dtos.Groups.GroupJoinRequestCreateDto;
import com.example.demo.Dtos.Groups.GroupJoinRequestResponseDto;
import com.example.demo.Models.Group;
import com.example.demo.Models.GroupJoinRequest;
import com.example.demo.Models.GroupJoinRequestStatus;
import com.example.demo.Models.MovementEntityType;
import com.example.demo.Models.User;
import com.example.demo.Repositories.GroupJoinRequestRepository;
import com.example.demo.Repositories.GroupRepository;
import com.example.demo.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class GroupJoinRequestService {

    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager em;
    @Autowired private CommunityService community;
    @Autowired private StateMovementService movementService;
    private final GroupRepository groupRepository;
    private final GroupJoinRequestRepository joinRequestRepository;
    private final UserRepository userRepository;
    private final GroupService groupService;

    public GroupJoinRequestService(GroupRepository groupRepository,
                                   GroupJoinRequestRepository joinRequestRepository,
                                   UserRepository userRepository,
                                   GroupService groupService) {
        this.groupRepository = groupRepository;
        this.joinRequestRepository = joinRequestRepository;
        this.userRepository = userRepository;
        this.groupService = groupService;
    }

    @Transactional
    public void submitRequest(Long requesterId, GroupJoinRequestCreateDto requestDto) {
        Objects.requireNonNull(requestDto, "Los datos de la solicitud son obligatorios.");
        if (requestDto.getGroupId() == null) {
            throw new IllegalArgumentException("Debes indicar la banda a la que quieres unirte.");
        }

        Group group = groupRepository.findByIdAndDeletedFalse(requestDto.getGroupId())
                .orElseThrow(() -> new IllegalArgumentException("No encontramos esa banda."));

        em.lock(group, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        em.refresh(group);
        User requester = userRepository.findByIdAndDeletedFalse(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el usuario."));

        if (group.getOwner().getId().equals(requesterId)) {
            throw new IllegalStateException("Ya eres el propietario de esta banda.");
        }

        if (group.hasMember(requester)) {
            throw new IllegalStateException("Ya formas parte de esta banda.");
        }

        if (group.isFull()) {
            throw new IllegalStateException("La banda alcanzó el máximo de integrantes.");
        }

        boolean alreadyPending = joinRequestRepository
                .findByGroupAndRequesterAndStatusAndDeletedFalse(group, requester, GroupJoinRequestStatus.PENDING)
                .isPresent();
        if (alreadyPending) {
            throw new IllegalStateException("Ya enviaste una solicitud pendiente para esta banda.");
        }

        if (requestDto.getMessage() != null && requestDto.getMessage().length() > 255) {
            throw new IllegalArgumentException("Mensaje demasiado largo");
        }
        GroupJoinRequest joinRequest = new GroupJoinRequest(group, requester, requestDto.getMessage());
        joinRequestRepository.saveAndFlush(joinRequest);

        String note = (requestDto.getMessage() != null && !requestDto.getMessage().isBlank())
                ? "Solicitud de ingreso enviada: " + requestDto.getMessage().trim()
                : "Solicitud de ingreso enviada";
        movementService.record(
                MovementEntityType.GROUP_JOIN_REQUEST,
                joinRequest.getId(),
                requester.getUsername() + " → " + group.getName(),
                null,
                GroupJoinRequestStatus.PENDING.name(),
                requester,
                note
        );
    }

    @Transactional(readOnly = true)
    public List<GroupJoinRequestResponseDto> getPendingRequestsForOwner(Long ownerId) {
        User owner = userRepository.findByIdAndDeletedFalse(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el usuario."));

        List<Group> ownedGroups = groupRepository.findAllByMembersContainsAndDeletedFalse(owner).stream()
                .filter(g -> community.permitted(g, ownerId, false))
                .toList();
        List<GroupJoinRequestResponseDto> responses = new ArrayList<>();

        for (Group group : ownedGroups) {
            List<GroupJoinRequest> pendingRequests = joinRequestRepository
                    .findAllByGroupAndStatusAndDeletedFalse(group, GroupJoinRequestStatus.PENDING);
            responses.addAll(pendingRequests.stream()
                    .map(this::toResponseDto)
                    .collect(Collectors.toList()));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public List<GroupJoinRequestResponseDto> getPendingRequestsByRequester(Long requesterId) {
        return em.createQuery(
                "select r from GroupJoinRequest r join fetch r.group g join fetch r.requester u " +
                "where r.deleted=false and g.deleted=false and u.id=:u and r.status=:st order by r.createdAt desc",
                GroupJoinRequest.class)
                .setParameter("u", requesterId)
                .setParameter("st", GroupJoinRequestStatus.PENDING)
                .getResultList()
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional
    public void approveRequest(Long requestId, Long ownerId) {
        GroupJoinRequest request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("No encontramos la solicitud."));

        em.lock(request.getGroup(), jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        em.refresh(request);
        validateOwnership(request, ownerId);

        if (request.getStatus() != GroupJoinRequestStatus.PENDING) {
            throw new IllegalStateException("La solicitud ya fue procesada.");
        }

        if (request.getGroup().isFull()) {
            throw new IllegalStateException("La banda alcanzó el máximo de integrantes.");
        }

        String previousStatus = request.getStatus().name();
        request.approve();
        joinRequestRepository.save(request);
        groupService.joinGroup(request.getGroup().getId(), request.getRequester().getId());

        User actor = userRepository.findByIdAndDeletedFalse(ownerId).orElseThrow();
        movementService.record(
                MovementEntityType.GROUP_JOIN_REQUEST,
                request.getId(),
                request.getRequester().getUsername() + " → " + request.getGroup().getName(),
                previousStatus,
                request.getStatus().name(),
                actor,
                "Solicitud aprobada e integrante sumado a la banda"
        );
    }

    @Transactional
    public void rejectRequest(Long requestId, Long ownerId) {
        GroupJoinRequest request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("No encontramos la solicitud."));

        em.lock(request.getGroup(), jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        em.refresh(request);
        validateOwnership(request, ownerId);

        if (request.getStatus() != GroupJoinRequestStatus.PENDING) {
            throw new IllegalStateException("La solicitud ya fue procesada.");
        }

        String previousStatus = request.getStatus().name();
        request.reject();
        joinRequestRepository.save(request);

        User actor = userRepository.findByIdAndDeletedFalse(ownerId).orElseThrow();
        movementService.record(
                MovementEntityType.GROUP_JOIN_REQUEST,
                request.getId(),
                request.getRequester().getUsername() + " → " + request.getGroup().getName(),
                previousStatus,
                request.getStatus().name(),
                actor,
                "Solicitud rechazada por el responsable de la banda"
        );
    }

    @Transactional
    public void cancelRequest(Long requestId, Long requesterId) {
        GroupJoinRequest request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("No encontramos la solicitud."));

        em.lock(request.getGroup(), jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        em.refresh(request);

        if (request.isDeleted() || !request.getRequester().getId().equals(requesterId)) {
            throw new IllegalStateException("Solo quien envió la solicitud puede cancelarla.");
        }

        if (request.getStatus() != GroupJoinRequestStatus.PENDING) {
            throw new IllegalStateException("La solicitud ya fue procesada.");
        }

        String previousStatus = request.getStatus().name();
        request.cancel();
        joinRequestRepository.save(request);

        User actor = userRepository.findByIdAndDeletedFalse(requesterId).orElseThrow();
        movementService.record(
                MovementEntityType.GROUP_JOIN_REQUEST,
                request.getId(),
                request.getRequester().getUsername() + " → " + request.getGroup().getName(),
                previousStatus,
                request.getStatus().name(),
                actor,
                "Solicitud cancelada por el propio solicitante"
        );
    }

    private void validateOwnership(GroupJoinRequest request, Long ownerId) {
        if (request.isDeleted() || !community.permitted(request.getGroup(), ownerId, false)) {
            throw new IllegalStateException("No tienes permisos para administrar esta solicitud.");
        }
    }

    private GroupJoinRequestResponseDto toResponseDto(GroupJoinRequest joinRequest) {
        GroupJoinRequestResponseDto dto = new GroupJoinRequestResponseDto();
        dto.setId(joinRequest.getId());
        dto.setGroupId(joinRequest.getGroup().getId());
        dto.setGroupName(joinRequest.getGroup().getName());
        dto.setRequesterId(joinRequest.getRequester().getId());
        dto.setRequesterUsername(joinRequest.getRequester().getUsername());
        dto.setMessage(joinRequest.getMessage());
        dto.setStatus(joinRequest.getStatus());
        dto.setCreatedAt(joinRequest.getCreatedAt());
        return dto;
    }
}
