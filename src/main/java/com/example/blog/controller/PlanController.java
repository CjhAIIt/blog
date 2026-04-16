package com.example.blog.controller;

import com.example.blog.model.Plan;
import com.example.blog.model.PlanAccessType;
import com.example.blog.model.PlanStatus;
import com.example.blog.model.Post;
import com.example.blog.model.User;
import com.example.blog.service.PlanService;
import com.example.blog.service.PostService;
import com.example.blog.service.UserService;
import com.example.blog.service.ViewModeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/plans")
public class PlanController {
    private final PlanService planService;
    private final PostService postService;
    private final UserService userService;
    private final ViewModeService viewModeService;

    public PlanController(PlanService planService,
                          PostService postService,
                          UserService userService,
                          ViewModeService viewModeService) {
        this.planService = planService;
        this.postService = postService;
        this.userService = userService;
        this.viewModeService = viewModeService;
    }

    @ModelAttribute("planAccessTypes")
    public PlanAccessType[] planAccessTypes() {
        return PlanAccessType.values();
    }

    @ModelAttribute("planStatuses")
    public PlanStatus[] planStatuses() {
        return PlanStatus.values();
    }

    @GetMapping
    public String listPlans(@RequestParam(defaultValue = "public") String filter,
                            @RequestParam(defaultValue = "0") int page,
                            Model model,
                            Principal principal) {
        User currentUser = principal == null ? null : userService.getByUsername(principal.getName());
        PageRequest pageable = PageRequest.of(page, 12, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Plan> plans = resolvePlanPage(filter, currentUser, pageable);

        model.addAttribute("plans", plans);
        model.addAttribute("currentPage", page);
        model.addAttribute("activeFilter", normalizeFilter(filter, currentUser));
        model.addAttribute("currentUser", currentUser);
        return view("plans/list");
    }

    @GetMapping("/new")
    public String newPlanForm(Model model, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", "请先登录后再创建计划");
            return "redirect:/login";
        }

        model.addAttribute("plan", new Plan());
        model.addAttribute("pageTitle", "新建计划");
        model.addAttribute("formAction", "/plans");
        return view("plans/form");
    }

    @PostMapping
    public String createPlan(@ModelAttribute Plan plan, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", "请先登录后再创建计划");
            return "redirect:/login";
        }

        User currentUser = userService.getByUsername(principal.getName());
        try {
            plan.setAuthor(currentUser);
            Plan savedPlan = planService.save(plan);
            redirectAttributes.addFlashAttribute("message", "计划已创建");
            return "redirect:/plans/" + savedPlan.getId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/plans/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String editPlanForm(@PathVariable Long id,
                               Model model,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", "请先登录后再编辑计划");
            return "redirect:/login";
        }

        User currentUser = userService.getByUsername(principal.getName());
        Plan plan = planService.findById(id).orElse(null);
        if (plan == null) {
            redirectAttributes.addFlashAttribute("error", "计划不存在");
            return "redirect:/plans";
        }
        if (!planService.canManage(plan, currentUser)) {
            redirectAttributes.addFlashAttribute("error", "你没有权限编辑这个计划");
            return "redirect:/plans/" + id;
        }

        model.addAttribute("plan", plan);
        model.addAttribute("pageTitle", "编辑计划");
        model.addAttribute("formAction", "/plans/" + id + "/edit");
        return view("plans/form");
    }

    @PostMapping("/{id}/edit")
    public String updatePlan(@PathVariable Long id,
                             @ModelAttribute Plan planForm,
                             Principal principal,
                             RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", "请先登录后再编辑计划");
            return "redirect:/login";
        }

        User currentUser = userService.getByUsername(principal.getName());
        Plan plan = planService.findById(id).orElse(null);
        if (plan == null) {
            redirectAttributes.addFlashAttribute("error", "计划不存在");
            return "redirect:/plans";
        }
        if (!planService.canManage(plan, currentUser)) {
            redirectAttributes.addFlashAttribute("error", "你没有权限编辑这个计划");
            return "redirect:/plans/" + id;
        }
        try {
            plan.setName(planForm.getName());
            plan.setDescription(planForm.getDescription());
            plan.setExpectedCount(planForm.getExpectedCount());
            plan.setStatus(planForm.getStatus());
            plan.setAccessType(planForm.getAccessType());
            planService.save(plan);

            redirectAttributes.addFlashAttribute("message", "计划已更新");
            return "redirect:/plans/" + id;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/plans/" + id + "/edit";
        }
    }

    @GetMapping("/{id}")
    public String viewPlan(@PathVariable Long id, Model model, Principal principal, RedirectAttributes redirectAttributes) {
        User currentUser = principal == null ? null : userService.getByUsername(principal.getName());
        Plan plan = planService.findAccessibleById(id, currentUser).orElse(null);
        if (plan == null) {
            redirectAttributes.addFlashAttribute("error", "计划不存在或你无权查看");
            return "redirect:/plans";
        }

        boolean canManage = planService.canManage(plan, currentUser);
        List<Post> planPosts = canManage ? postService.findByPlanId(id) : postService.findPublishedByPlanId(id);
        int publishedCount = planService.getPublishedPostCount(id);

        int progress = plan.getExpectedCount() > 0 ? (publishedCount * 100) / plan.getExpectedCount() : 0;

        model.addAttribute("plan", plan);
        model.addAttribute("posts", planPosts);
        model.addAttribute("publishedCount", publishedCount);
        model.addAttribute("progress", Math.min(progress, 100));
        model.addAttribute("isAuthor", canManage);
        model.addAttribute("canJoinPlan", planService.canJoin(plan, currentUser));
        model.addAttribute("canCreatePostInPlan", currentUser != null
                && userService.canWritePosts(currentUser)
                && planService.canJoin(plan, currentUser));
        return view("plans/view");
    }

    private Page<Plan> resolvePlanPage(String filter, User currentUser, PageRequest pageable) {
        String normalizedFilter = normalizeFilter(filter, currentUser);
        if ("mine".equals(normalizedFilter) && currentUser != null) {
            return planService.getUserPlans(currentUser.getId(), pageable);
        }
        if ("collaborative".equals(normalizedFilter)) {
            return planService.getCollaborativePlans(pageable);
        }
        return planService.getPublicPlans(pageable);
    }

    private String normalizeFilter(String filter, User currentUser) {
        if ("mine".equalsIgnoreCase(filter) && currentUser != null) {
            return "mine";
        }
        if ("collaborative".equalsIgnoreCase(filter)) {
            return "collaborative";
        }
        return "public";
    }

    private String view(String name) {
        return viewModeService.view(name);
    }
}
