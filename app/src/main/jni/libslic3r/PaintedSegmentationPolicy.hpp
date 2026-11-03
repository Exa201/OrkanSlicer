#ifndef slic3r_PaintedSegmentationPolicy_hpp_
#define slic3r_PaintedSegmentationPolicy_hpp_

#include <cstddef>

namespace Slic3r {

// Facet state 0 is unpainted; the remaining states correspond to configured filaments.
// Deliberately do not size this from filament_colour: imported profiles may have a short palette.
inline size_t painted_segmentation_state_count(size_t filament_count)
{
    return filament_count + 1;
}

} // namespace Slic3r

#endif // slic3r_PaintedSegmentationPolicy_hpp_
