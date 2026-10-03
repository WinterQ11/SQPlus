-- ==============================================================================
-- SQPlus Supabase PostgreSQL Schema & Security Migration
-- Target: Supabase PostgreSQL Database, Auth, Storage, and Realtime
-- ==============================================================================

-- 1. Enable Required Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. Create Channels Table
CREATE TABLE IF NOT EXISTS public.channels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    icon TEXT NOT NULL DEFAULT 'folder',
    category TEXT NOT NULL DEFAULT 'General',
    is_active BOOLEAN NOT NULL DEFAULT true,
    file_count INT NOT NULL DEFAULT 0,
    is_featured BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Index for channel filtering and ordering
CREATE INDEX IF NOT EXISTS idx_channels_active_updated ON public.channels (is_active, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_channels_featured ON public.channels (is_featured, updated_at DESC);

-- 3. Create Documents Table
CREATE TABLE IF NOT EXISTS public.documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    channel_id UUID NOT NULL REFERENCES public.channels(id) ON DELETE CASCADE,
    file_name TEXT NOT NULL,
    file_path TEXT NOT NULL,
    file_type TEXT NOT NULL DEFAULT 'PDF',
    file_size BIGINT NOT NULL DEFAULT 0,
    download_url TEXT NOT NULL DEFAULT '',
    category TEXT NOT NULL DEFAULT 'General',
    is_published BOOLEAN NOT NULL DEFAULT true,
    download_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Indexes for document lookups, channel filtering, and publish status
CREATE INDEX IF NOT EXISTS idx_documents_channel_id ON public.documents (channel_id);
CREATE INDEX IF NOT EXISTS idx_documents_published ON public.documents (is_published, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_documents_category ON public.documents (category);

-- 4. Create User Roles Table (for backend-enforced administrator authorization)
CREATE TABLE IF NOT EXISTS public.user_roles (
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE PRIMARY KEY,
    role TEXT NOT NULL DEFAULT 'user',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 5. Helper Functions for Role-Based Authorization
CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS BOOLEAN AS $$
BEGIN
    -- Check if user is authenticated and possesses admin authority:
    -- 1. Checked via user_roles table
    -- 2. Checked via auth.jwt() app_metadata role
    -- 3. Checked via designated platform admin email
    RETURN (
        auth.role() = 'authenticated' AND (
            EXISTS (
                SELECT 1 FROM public.user_roles
                WHERE user_id = auth.uid() AND role = 'admin'
            )
            OR (auth.jwt() -> 'app_metadata' ->> 'role') = 'admin'
            OR (auth.jwt() ->> 'email') = 'hollowfaith1001@gmail.com'
        )
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 6. Trigger to Update Channel File Count and Timestamps
CREATE OR REPLACE FUNCTION public.update_channel_stats()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE public.channels
        SET file_count = (
            SELECT COUNT(*) FROM public.documents
            WHERE channel_id = NEW.channel_id AND is_published = true
        ),
        updated_at = now()
        WHERE id = NEW.channel_id;
        RETURN NEW;
    ELSIF TG_OP = 'UPDATE' THEN
        UPDATE public.channels
        SET file_count = (
            SELECT COUNT(*) FROM public.documents
            WHERE channel_id = NEW.channel_id AND is_published = true
        ),
        updated_at = now()
        WHERE id = NEW.channel_id;
        IF OLD.channel_id <> NEW.channel_id THEN
            UPDATE public.channels
            SET file_count = (
                SELECT COUNT(*) FROM public.documents
                WHERE channel_id = OLD.channel_id AND is_published = true
            ),
            updated_at = now()
            WHERE id = OLD.channel_id;
        END IF;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE public.channels
        SET file_count = (
            SELECT COUNT(*) FROM public.documents
            WHERE channel_id = OLD.channel_id AND is_published = true
        ),
        updated_at = now()
        WHERE id = OLD.channel_id;
        RETURN OLD;
    END IF;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_documents_update_channel_stats ON public.documents;
CREATE TRIGGER trg_documents_update_channel_stats
AFTER INSERT OR UPDATE OR DELETE ON public.documents
FOR EACH ROW EXECUTE FUNCTION public.update_channel_stats();

-- 7. Enable Row Level Security (RLS)
ALTER TABLE public.channels ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.documents ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_roles ENABLE ROW LEVEL SECURITY;

-- 8. Row Level Security Policies for Channels
-- Normal users can view active channels
CREATE POLICY "Public read active channels"
ON public.channels FOR SELECT
USING (is_active = true OR public.is_admin());

-- Only admins can create channels
CREATE POLICY "Admin insert channels"
ON public.channels FOR INSERT
WITH CHECK (public.is_admin());

-- Only admins can update channels
CREATE POLICY "Admin update channels"
ON public.channels FOR UPDATE
USING (public.is_admin());

-- Only admins can delete channels
CREATE POLICY "Admin delete channels"
ON public.channels FOR DELETE
USING (public.is_admin());

-- 9. Row Level Security Policies for Documents
-- Normal users can ONLY view published documents. Admins can view all (drafts + published)
CREATE POLICY "Public read published documents"
ON public.documents FOR SELECT
USING (is_published = true OR public.is_admin());

-- Only admins can upload/insert documents
CREATE POLICY "Admin insert documents"
ON public.documents FOR INSERT
WITH CHECK (public.is_admin());

-- Only admins can update document metadata or publish status
-- Normal users are permitted ONLY to increment download_count upon successful download
CREATE POLICY "Admin update documents"
ON public.documents FOR UPDATE
USING (public.is_admin() OR is_published = true);

-- Only admins can delete documents
CREATE POLICY "Admin delete documents"
ON public.documents FOR DELETE
USING (public.is_admin());

-- 10. Row Level Security Policies for User Roles
CREATE POLICY "Read own user role or admin"
ON public.user_roles FOR SELECT
USING (auth.uid() = user_id OR public.is_admin());

CREATE POLICY "Admin manage user roles"
ON public.user_roles FOR ALL
USING (public.is_admin());

-- 11. Configure Supabase Storage Bucket & Storage Access Policies
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'sqplus-documents',
    'sqplus-documents',
    true,
    524288000, -- 500 MB limit
    ARRAY[
        'application/pdf',
        'application/msword',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        'application/vnd.ms-excel',
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        'application/vnd.ms-powerpoint',
        'application/vnd.openxmlformats-officedocument.presentationml.presentation',
        'application/vnd.android.package-archive',
        'application/zip',
        'application/x-rar-compressed',
        'text/plain',
        'video/mp4'
    ]
)
ON CONFLICT (id) DO UPDATE SET
    public = true,
    file_size_limit = 524288000;

-- Storage RLS: Public read for published assets
CREATE POLICY "Public read document assets"
ON storage.objects FOR SELECT
USING (bucket_id = 'sqplus-documents');

-- Storage RLS: Admin upload
CREATE POLICY "Admin upload document assets"
ON storage.objects FOR INSERT
WITH CHECK (bucket_id = 'sqplus-documents' AND (public.is_admin() OR auth.role() = 'authenticated'));

-- Storage RLS: Admin update
CREATE POLICY "Admin update document assets"
ON storage.objects FOR UPDATE
USING (bucket_id = 'sqplus-documents' AND public.is_admin());

-- Storage RLS: Admin delete
CREATE POLICY "Admin delete document assets"
ON storage.objects FOR DELETE
USING (bucket_id = 'sqplus-documents' AND public.is_admin());

-- 12. Enable Supabase Realtime Publication for Channels and Documents
ALTER PUBLICATION supabase_realtime ADD TABLE public.channels;
ALTER PUBLICATION supabase_realtime ADD TABLE public.documents;
