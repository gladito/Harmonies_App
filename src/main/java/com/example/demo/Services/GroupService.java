package com.example.demo.Services;

import com.example.demo.Dtos.Groups.GroupCreateRequestDto;
import com.example.demo.Dtos.Groups.GroupSummaryDto;
import com.example.demo.Dtos.Groups.MemberDto;
import com.example.demo.Models.Group;
import com.example.demo.Models.User;
import com.example.demo.Repositories.GroupRepository;
import com.example.demo.Repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class GroupService {

    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager em;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public GroupService(GroupRepository groupRepository, UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly=true)
    public List<GroupSummaryDto> getGroupsForUser(Long userId) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el usuario."));

        List<Group> groups = groupRepository.findAllByMembersContainsAndDeletedFalse(user);
        return groups.stream()
                .map(group -> toSummaryDto(group, user))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GroupSummaryDto> getAvailableGroupsForUser(Long userId) {
        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el usuario."));

        return groupRepository.findAll().stream()
                .filter(group -> !group.isDeleted())
                .filter(group -> !group.hasMember(user))
                .map(group -> toSummaryDto(group, user))
                .collect(Collectors.toList());
    }

    @Transactional
    public GroupSummaryDto createGroup(Long ownerId, GroupCreateRequestDto request) {
        validateCreateRequest(request);

        User owner = userRepository.findByIdAndDeletedFalse(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el usuario."));

        if (groupRepository.existsByNameAndDeletedFalse(request.getName())) {
            throw new IllegalArgumentException("Ya existe una banda con ese nombre.");
        }

        Group group = new Group(request.getName(), request.getDescription(), owner);
        if (request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()) {
            group.setAvatarUrl(request.getAvatarUrl().trim());
        }
        groupRepository.save(group);
        return toSummaryDto(group, owner);
    }

    @Transactional
    public void joinGroup(Long groupId, Long userId) {
        Group group = groupRepository.findByIdAndDeletedFalse(groupId)
                .orElseThrow(() -> new IllegalArgumentException("No encontramos esa banda."));

        em.lock(group,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        em.refresh(group);

        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el usuario."));

        if (group.hasMember(user)) {
            throw new IllegalStateException("Ya eres parte de esta banda.");
        }
        if (group.isFull()) {
            throw new IllegalStateException("La banda ya alcanzó el máximo de integrantes.");
        }

        group.addMember(user);
        groupRepository.save(group);
    }

    @Transactional
    public void leaveGroup(Long groupId, Long userId) {
        Group group = groupRepository.findByIdAndDeletedFalse(groupId)
                .orElseThrow(() -> new IllegalArgumentException("No encontramos esa banda."));

        User user = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el usuario."));

        if (group.getOwner().getId().equals(userId)) {
            throw new IllegalStateException("El propietario no puede abandonar la banda. Considera eliminarla.");
        }

        if (!group.hasMember(user)) {
            throw new IllegalStateException("No perteneces a esta banda.");
        }

        em.createQuery("delete from Membership m where m.group.id=:g and m.user.id=:u").setParameter("g",groupId).setParameter("u",userId).executeUpdate();
        group.removeMember(user);
        groupRepository.save(group);
    }

    @Transactional
    public void deleteGroup(Long groupId, Long ownerId) {
        Group group = groupRepository.findByIdAndDeletedFalse(groupId)
                .orElseThrow(() -> new IllegalArgumentException("No encontramos esa banda."));

        if (!group.getOwner().getId().equals(ownerId)) {
            throw new IllegalStateException("Solo el propietario puede eliminar la banda.");
        }

        group.markAsDeleted();
        group.getMembers().forEach(member -> member.removeGroup(group));
        groupRepository.save(group);
    }

    @Transactional(readOnly = true)
    public GroupCreateRequestDto getGroupForEdit(Long groupId, Long ownerId) {
        Group group = groupRepository.findByIdAndDeletedFalse(groupId)
                .orElseThrow(() -> new IllegalArgumentException("No encontramos esa banda."));

        if (!group.getOwner().getId().equals(ownerId)) {
            throw new IllegalStateException("Solo el propietario puede editar la banda.");
        }

        GroupCreateRequestDto dto = new GroupCreateRequestDto();
        dto.setName(group.getName());
        dto.setDescription(group.getDescription());
        dto.setAvatarUrl(group.getAvatarUrl());
        return dto;
    }

    @Transactional
    public GroupSummaryDto updateGroup(Long groupId, Long ownerId, GroupCreateRequestDto request) {
        Group group = groupRepository.findByIdAndDeletedFalse(groupId)
                .orElseThrow(() -> new IllegalArgumentException("No encontramos esa banda."));

        if (!group.getOwner().getId().equals(ownerId)) {
            throw new IllegalStateException("Solo el propietario puede editar la banda.");
        }

        validateCreateRequest(request);

        if (!group.getName().equals(request.getName())
                && groupRepository.existsByNameAndDeletedFalse(request.getName())) {
            throw new IllegalArgumentException("Ya existe una banda con ese nombre.");
        }

        group.setName(request.getName());
        group.setDescription(request.getDescription());
        if (request.getAvatarUrl() != null) {
            group.setAvatarUrl(request.getAvatarUrl().isBlank() ? null : request.getAvatarUrl().trim());
        }
        groupRepository.save(group);
        return toSummaryDto(group, group.getOwner());
    }

    private void validateCreateRequest(GroupCreateRequestDto request) {
        Objects.requireNonNull(request, "Los datos de la banda son obligatorios");

        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("El nombre de la banda es obligatorio.");
        }

        if (request.getName().length() > 80) {
            throw new IllegalArgumentException("El nombre de la banda no puede superar los 80 caracteres.");
        }

        if (request.getDescription() != null && request.getDescription().length() > 255) {
            throw new IllegalArgumentException("La descripción no puede superar los 255 caracteres.");
        }
    }

    private GroupSummaryDto toSummaryDto(Group group, User user) {
        boolean isOwner = group.getOwner() != null && group.getOwner().getId().equals(user.getId());
        GroupSummaryDto dto = new GroupSummaryDto(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.getMemberCount(),
                isOwner
        );
        dto.setAvatarUrl(group.getAvatarUrl());
        dto.setMembers(group.getMembers().stream()
                .filter(member -> !member.isDeleted())
                .map(member -> new MemberDto(
                        member.getId(),
                        member.getUsername(),
                        group.getOwner() != null && group.getOwner().getId().equals(member.getId()),
                        member.getProfile() != null ? member.getProfile().getAvatarUrl() : null))
                .collect(Collectors.toList()));
        return dto;
    }
}
