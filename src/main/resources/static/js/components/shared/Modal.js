const { createApp, ref } = Vue;

createApp({
    setup() {
        const isOpen = ref(false);
        const post = ref(null);

        function open(postData) {
            post.value = postData;
            isOpen.value = true;
            document.body.style.overflow = 'hidden';
        }

        function close() {
            isOpen.value = false;
            post.value = null;
            document.body.style.overflow = '';
        }

        // Expose open/close globally so Thymeleaf-rendered buttons can call them
        window.__blogModal = { open, close };

        return { isOpen, post, close };
    },
    template: `
        <div v-if="isOpen" class="ll-modal-overlay" @click.self="close" role="dialog" aria-modal="true">
            <div class="ll-modal">
                <button class="ll-modal-close" @click="close" aria-label="关闭">
                    <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
                        <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
                    </svg>
                </button>
                <template v-if="post">
                    <div class="ll-modal-tags">
                        <span v-if="post.featured" class="ll-tag ll-tag--featured">精选</span>
                        <span v-if="post.pinned" class="ll-tag ll-tag--pinned">置顶</span>
                    </div>
                    <h2 class="ll-modal-title">{{ post.title }}</h2>
                    <p class="ll-modal-excerpt">{{ post.excerpt }}</p>
                    <div class="ll-modal-meta">
                        <span>{{ post.author }}</span>
                        <span>{{ post.likeCount }} 赞</span>
                    </div>
                    <a :href="'/posts/' + post.id" class="ll-btn" style="margin-top:16px;display:inline-block">细读芳华</a>
                </template>
            </div>
        </div>
    `
}).mount('#modal-app');
